package com.ef.repository;

import com.ef.entity.AccessLog;
import org.springframework.data.repository.CrudRepository;

public interface AccessLogRepository extends CrudRepository<AccessLog, Integer> {

}
