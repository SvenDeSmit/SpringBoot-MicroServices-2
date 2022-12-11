package be.snife.sbms.productcomposite;

import org.springdoc.core.customizers.OpenApiBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class AppConfiguration {
	
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
}
