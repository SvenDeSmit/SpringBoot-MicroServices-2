package be.snife.sbms.recommendation.services;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.core.recommendation.RecommendationService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.event.Event.Type;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.recommendation.persistence.RecommendationEntity;
import be.snife.sbms.recommendation.persistence.RecommendationRepository;
import be.snife.sbms.util.http.ServiceUtil;

@RestController
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

	private final ServiceUtil serviceUtil;
	private final RecommendationRepository repository;
	private final RecommendationMapper mapper;
	private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;

	@Autowired
	public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper,ServiceUtil serviceUtil, StreamBridge streamBridge,@Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {
		this.serviceUtil = serviceUtil;
		this.mapper = mapper;
		this.repository = repository;
		this.streamBridge = streamBridge;
		this.publishEventScheduler = publishEventScheduler;
	}

	@Override
	public Mono<Recommendation> createRecommendation(Recommendation body) {
		if (body.getProductId() < 1) {
			throw new InvalidInputException("Invalid productId: " + body.getProductId());
		}

		if (body.getRecommendationId() < 1) {
			throw new InvalidInputException("Invalid recommendationId: " + body.getRecommendationId());
		}
		
		log.debug("Creating Recommendation entity for productId {} and RecommendationId {} ...", body.getProductId(), body.getRecommendationId());
		RecommendationEntity entity = mapper.apiToEntity(body);
		Mono<Recommendation> newEntity = repository.save(entity)
				//.log(log.getName(), FINE)
				.onErrorMap(DuplicateKeyException.class,
						ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
				.map(ent -> mapper.entityToApi(ent));
		
		return newEntity;
	}

	@Override
	public Flux<Recommendation> getRecommendations(int productId) {
		log.debug("Getting Recommendations for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}
		
		Flux<Recommendation> recstream = repository.findByProductId(productId)
				.map(r -> mapper.entityToApi(r))
				.map(e -> setServiceAddress(e));
		return recstream;	
	}

	@Override
	public Mono<Void> deleteRecommendations(int productId) {
		log.debug("Deleting Recommendations with productId: {}",productId);
		return repository.deleteAll(repository.findByProductId(productId));
	}
	
	private Recommendation setServiceAddress(Recommendation e) {
		e.setServiceAddress(serviceUtil.getServiceAddress());
		return e;
	}
	
	@Override
	public Mono<Void> publishRecommendationEvent(@RequestBody Event<Integer, Recommendation> event) {
	    return Mono.fromCallable(() -> {
	        sendMessage("recommendations-out-0", new Event<Integer, Recommendation>(event.getEventType(), event.getKey(), event.getData()));
	        return event;
	      }).then()
	      .subscribeOn(publishEventScheduler);
	}
	

    private void sendMessage(String bindingName, Event<Integer, Recommendation> event) {
	    log.debug("Sending a Recommendation {} message with ID {} to {}", event.getEventType(), event.getKey(),bindingName);
        Message<Event<Integer, Recommendation>> message = MessageBuilder.withPayload(event)
            .setHeader("partitionKey", event.getKey())
            .build();
        streamBridge.send(bindingName, message);
	    log.debug("{} Recommendation Message with ID {} sent to {}", event.getEventType(), event.getKey(), bindingName);

  }
	
	
}
