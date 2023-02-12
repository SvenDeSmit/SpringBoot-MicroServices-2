package be.snife.sbms.productcomposite;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import be.snife.sbms.productcomposite.services.ProductCompositeIntegration;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("be.snife.sbms")
@Slf4j
public class ProductCompositeServiceApplication {

	private final Integer threadPoolSize;
	private final Integer taskQueueSize;

	@Autowired
	public ProductCompositeServiceApplication(@Value("${app.threadPoolSize:10}") Integer threadPoolSize,
			@Value("${app.taskQueueSize:100}") Integer taskQueueSize) {
		this.threadPoolSize = threadPoolSize;
		this.taskQueueSize = taskQueueSize;
	}

	@Bean
	public Scheduler publishEventScheduler() {
		log.info("Creates a messagingScheduler with connectionPoolSize = {}", threadPoolSize);
		return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool");
	}

	@Autowired
	ProductCompositeIntegration integration;

/*	
	@Bean
	ReactiveHealthContributor coreServices() {

		final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();

		registry.put("product", () -> integration.getProductHealth());
		registry.put("recommendation", () -> integration.getRecommendationHealth());
		registry.put("review", () -> integration.getReviewHealth());

		return CompositeReactiveHealthContributor.fromMap(registry);
	}
*/
	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
		log.info("Starting ProductCompositeServiceApplication microservice ...");

	}

	@Bean
	public OpenAPI getOpenApiDocumentation() {
		OpenAPI general = new OpenAPI()
				.info(new Info().title("ProductComposite API").description("ProductComposite API").version("1.0.0")
						.contact(new Contact().name("Sven De Smit").url("https://snife.be/microservices.html")
								.email("sven.desmit@telenet.be"))
						.termsOfService("my terms of service ...")
						.license(new License().name("my license ...")
								.url("https://snife.be/microservices-licenses.html")))
				.externalDocs(new ExternalDocumentation().description("my wiki page ...").url("my wiki url ..."));
		return general;
	}

}
