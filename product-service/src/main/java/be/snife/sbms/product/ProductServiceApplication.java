package be.snife.sbms.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import be.snife.sbms.product.persistence.ProductEntity;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan("be.snife.sbms")
@Slf4j
public class ProductServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);
		log.info("Starting ProductServiceApplication microservice ...");

		String mongodDbHost = ctx.getEnvironment().getProperty("spring.data.mongodb.host");
		String mongodDbPort = ctx.getEnvironment().getProperty("spring.data.mongodb.port");
		log.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);

	}

	@Autowired(required = true)
	ReactiveMongoOperations mongoTemplate;

	
	@EventListener(ContextRefreshedEvent.class)
	public void initIndicesAfterStartup() {

		MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate
				.getConverter().getMappingContext();
		IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

		ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
		resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.ensureIndex(e).block());
	}

}
