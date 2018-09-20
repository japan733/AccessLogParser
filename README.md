# AccessLogParser

Build the project using below command

gradlew clean build --refresh-dependencies

After build is successfully completed. Go to below path in the project.

build/libs

From this path execute the below command to execute the solution and see the result

java -jar parser.jar --accesslog=/Users/japantrivedi/Documents/WalletHub_Test/access.log --startDate=2017-01-01.00:00:00 --duration=daily --threshold=500 
