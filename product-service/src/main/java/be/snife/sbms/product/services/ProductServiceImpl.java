package be.snife.sbms.product.services;

import static java.util.logging.Level.FINE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.product.ProductService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.event.Event.Type;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.api.exceptions.NotFoundException;
import be.snife.sbms.product.persistence.ProductEntity;
import be.snife.sbms.product.persistence.ProductRepository;
import be.snife.sbms.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

	private final ServiceUtil serviceUtil;
	private final ProductRepository repository;
	private final ProductMapper mapper;
	private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;



	@Autowired
	public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil, StreamBridge streamBridge,@Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
		this.streamBridge = streamBridge;
		this.publishEventScheduler = publishEventScheduler;
	}

	@Override
	public Mono<Product> createProduct(Product body) {
		if (body.getProductId() < 1) {
			throw new InvalidInputException("Invalid productId: " + body.getProductId());
		}

		log.debug("Creating Product entity for productId {} ...", body.getProductId());
		ProductEntity entity = mapper.apiToEntity(body);
		Mono<Product> newEntity = repository.save(entity)
				//.log(log.getName(), FINE)
				.onErrorMap(DuplicateKeyException.class,
						ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
				.map(ent -> mapper.entityToApi(ent));

		return newEntity;
	}

	@Override
	public Mono<Product> getProduct(int productId) {
		log.debug("Getting Product with productId = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		log.debug("Getting Product entity for productId {} ...", productId);
		
		Mono<Product> prod = repository.findByProductId(productId)
				.switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
				//.log(log.getName(), FINE)
				.map(p -> mapper.entityToApi(p))
				.map(e -> setServiceAddress(e));
		
		return prod;
	}

	@Override
	public Mono<Void> deleteProduct(int productId) {
		log.debug("Deleting Product entity with productId: {} ...", productId);

		return repository.findByProductId(productId)
				//.log(log.getName(), FINE)
				.map(p -> repository.delete(p))
				.flatMap(p -> p);
	}

	private Product setServiceAddress(Product e) {
		e.setServiceAddress(serviceUtil.getServiceAddress());
		return e;
	}
	
	@Override
	public Mono<Void> publishProductEvent(@RequestBody Event<Integer, Product> event) {
	    return Mono.fromCallable(() -> {
	        sendMessage("products-out-0", new Event<Integer, Product>(event.getEventType(), event.getKey(), event.getData()));
	        return event;
	      }).then()
	      .subscribeOn(publishEventScheduler);
	}
	

    private void sendMessage(String bindingName, Event<Integer, Product> event) {
	    log.debug("Sending a Product {} message with ID {} to {}", event.getEventType(), event.getKey(),bindingName);
        Message<Event<Integer, Product>> message = MessageBuilder.withPayload(event)
            .setHeader("partitionKey", event.getKey())
            .build();
        streamBridge.send(bindingName, message);
	    log.debug("{} Product Message with ID {} sent to {}", event.getEventType(), event.getEventType(), bindingName);

  }
	

}
