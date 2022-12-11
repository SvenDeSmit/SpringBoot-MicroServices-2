package be.snife.sbms.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan("be.snife.sbms")
@Slf4j
public class ReviewServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);
		log.info("Starting ReviewServiceApplication microservice ...");
	    String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
	    log.info("Connected to MySQL: " + mysqlUri);		
	}
	
}
