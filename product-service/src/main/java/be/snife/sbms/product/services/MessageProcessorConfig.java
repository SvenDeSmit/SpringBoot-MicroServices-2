package be.snife.sbms.product.services;

import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.product.ProductService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MessageProcessorConfig {

  private final ProductService productService;

  @Autowired
  public MessageProcessorConfig(ProductService productService) {
    this.productService = productService;
  }

  @Bean
  public Consumer<Event<Integer, Product>> messageProcessor() {
    return event -> {
      log.info("Product Event received at {}...", event.getEventCreatedAt());
      log.debug("[{} , {}, {}, {}]",event.getEventCreatedAt(),event.getEventType(),event.getKey(),event.getData());

      switch (event.getEventType()) {

        case CREATE:
          Product product = event.getData();
          log.info("Creating Product with ID: {}", product.getProductId());
          productService.createProduct(product).block();
          break;

        case DELETE:
          int productId = event.getKey();
          log.info("Deleting Product with ID: {}", productId);
          productService.deleteProduct(productId).block();
          break;

        default:
          String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
          log.warn(errorMessage);
          throw new EventProcessingException(errorMessage);
      }

      log.info("Message processing done!");

    };
  }
}
