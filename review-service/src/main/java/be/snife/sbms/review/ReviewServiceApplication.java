package be.snife.sbms.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("be.snife.sbms")
@Slf4j
public class ReviewServiceApplication {

	private final Integer threadPoolSize;
	private final Integer taskQueueSize;

	@Autowired
	public ReviewServiceApplication(@Value("${app.threadPoolSize:10}") Integer threadPoolSize,
			@Value("${app.taskQueueSize:100}") Integer taskQueueSize) {
		this.threadPoolSize = threadPoolSize;
		this.taskQueueSize = taskQueueSize;
	}

	@Bean
	public Scheduler jdbcScheduler() {
		log.info("Creates a jdbcScheduler with thread pool size = {}", threadPoolSize);
		return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);
		log.info("Starting ReviewServiceApplication microservice ...");
		String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
		log.info("Connected to MySQL: " + mysqlUri);
	}

}
