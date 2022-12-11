package be.snife.sbms.productcomposite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan("be.snife.sbms")
@Slf4j
public class ProductCompositeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
		log.info("Starting ProductCompositeServiceApplication microservice ...");

	}

    @Bean
    public OpenAPI getOpenApiDocumentation( ) {
    	OpenAPI general = new OpenAPI()
    			.info(new Info()
    					.title("ProductComposite API")
    					.description("ProductComposite API")
    					.version("1.0.0")
    					.contact(new Contact()
    							.name("Sven De Smit")
    							.url("https://snife.be/microservices.html")
    							.email("sven.desmit@telenet.be")
    							)
    					.termsOfService("my terms of service ...")
    					.license(new License()
    							.name("my license ...")
    							.url("https://snife.be/microservices-licenses.html")
    							)
    					).externalDocs(new ExternalDocumentation()
    							.description("my wiki page ...")
    							.url("my wiki url ...")
    							);
    	return general;
    }
    	
}
