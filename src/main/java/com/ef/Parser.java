package com.ef;

import com.ef.entity.AccessLog;
import com.ef.entity.BlockedAddress;
import com.ef.repository.AccessLogRepository;
import com.ef.repository.BlockedAddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class Parser implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private BlockedAddressRepository blockedAddressRepository;

    private List<AccessLog> accessLogList;

    public static void main(String[] args) {
        SpringApplication parserApp = new SpringApplication(Parser.class);
        parserApp.run(args);
    }

    @Override
    public void run(ApplicationArguments args) {

        LOGGER.info("Welcome to Access Log Parser.");
        LOGGER.info("Application starting with following options");

        args.getOptionNames().stream().forEach(name -> LOGGER.info("Argument " + name + "=" + args.getOptionValues(name)));

        Resource logFile = resourceLoader.getResource("classpath:access.log");

        if(logFile != null) {
            try {
//                ClassPathResource classPathResource = new ClassPathResource("access.log");
                accessLogList = new ArrayList<>();

//                String splitURL[] = classPathResource.getURL().toString().split("!");
//                FileSystem fs = FileSystems.newFileSystem(URI.create(splitURL[0]), new HashMap<String,String>());
//                Files.readAllLines(fs.getPath(splitURL[1],splitURL[2])).stream().forEach(this::addToAccessLogList);
                Files.readAllLines(Paths.get(args.getOptionValues("accesslog").get(0))).stream().forEach(this::addToAccessLogList);
                accessLogList.removeIf(Objects::isNull);
                LOGGER.info("Inserting records to DB :: " + accessLogList.size());
                accessLogRepository.saveAll(accessLogList);
                Timestamp startDate = Timestamp.valueOf(LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss").parse(args.getOptionValues("startDate").get(0))));
                Timestamp endDate = new Timestamp(startDate.getTime() + (1000 * 60 * 60 * (args.getOptionValues("duration").get(0).equalsIgnoreCase("daily") ? 24 : 1)));
                LOGGER.info("****** Following are the blocked IP Addresses *******");
                accessLogList.stream().filter(accessLog -> accessLog.getAccessTime().after(startDate) && accessLog.getAccessTime().before(endDate)).map(accessLog -> accessLog.getIpAddress()).collect(Collectors.toMap(ip -> ip, ip -> 1, Integer::sum)).entrySet().stream().filter(ip -> ip.getValue()>Integer.valueOf(args.getOptionValues("threshold").get(0))).forEach(blockedIp -> {
                    StringBuilder reasonBuilder = new StringBuilder();
                    reasonBuilder.append(blockedIp.getKey() + " Address blocked. Accessed server " + blockedIp.getValue());
                    reasonBuilder.append(" times between " + startDate + " and " + endDate);
                    reasonBuilder.append(" which is more than threshold " + args.getOptionValues("threshold").get(0));
                    LOGGER.info(reasonBuilder.toString());
                    blockedAddressRepository.save(new BlockedAddress(blockedIp.getKey(), reasonBuilder.toString()));
                });

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addToAccessLogList(String line) {
        String[] accessLogValues = line.split("\\|");
        if(accessLogValues.length==5) {
            AccessLog accessLog = new AccessLog();
            accessLog.setAccessTime(Timestamp.valueOf(LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").parse(accessLogValues[0]))));
            accessLog.setIpAddress(accessLogValues[1]);
            accessLog.setRequest(accessLogValues[2]);
            accessLog.setStatus(accessLogValues[3]);
            accessLog.setUserAgent(accessLogValues[4]);
            accessLogList.add(accessLog);
        }
    }
}
