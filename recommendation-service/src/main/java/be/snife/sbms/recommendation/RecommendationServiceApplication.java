package be.snife.sbms.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan("be.snife.sbms")
@Slf4j
public class RecommendationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecommendationServiceApplication.class, args);
		log.info("Starting RecommendationServiceApplication microservice ...");

	}

}
