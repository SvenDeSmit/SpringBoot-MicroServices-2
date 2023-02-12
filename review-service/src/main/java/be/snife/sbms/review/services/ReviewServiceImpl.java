package be.snife.sbms.review.services;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.api.core.review.ReviewService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.event.Event.Type;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.review.persistence.ReviewEntity;
import be.snife.sbms.review.persistence.ReviewRepository;
import be.snife.sbms.util.http.ServiceUtil;

@RestController
@Slf4j
public class ReviewServiceImpl implements ReviewService {

	private final ServiceUtil serviceUtil;
	private final ReviewRepository repository;
	private final ReviewMapper mapper;
	private final Scheduler jdbcScheduler;
	private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;
	

	@Autowired
	public ReviewServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, ReviewRepository repository,
			ReviewMapper mapper, ServiceUtil serviceUtil, StreamBridge streamBridge,@Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {
		this.jdbcScheduler = jdbcScheduler;
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
		this.streamBridge = streamBridge;
		this.publishEventScheduler = publishEventScheduler;
	}

	@Override
	public Mono<Review> createReview(Review body) {

		if (body.getProductId() < 1) {
			throw new InvalidInputException("Invalid productId: " + body.getProductId());
		}
		return Mono.fromCallable(() -> internalCreateReview(body)).subscribeOn(jdbcScheduler);
	}

	public Review internalCreateReview(Review body) {
		log.debug("Creating Review for Product with ID = {} and ReviewID {} on {}", body.getProductId(),
				body.getReviewId(), serviceUtil.getServiceAddress());

		try {
			ReviewEntity entity = mapper.apiToEntity(body);
			ReviewEntity newEntity = repository.save(entity);

			log.debug("Review for Product with ID = {} and ReviewID {} is stored", body.getProductId(),body.getReviewId());
			Review res = mapper.entityToApi(newEntity);
			res.setServiceAddress(serviceUtil.getServiceAddress());

			return res;

		} catch (DataIntegrityViolationException dive) {
			throw new InvalidInputException(
					"Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
		}
	}

	@Override
	public Flux<Review> getReviews(int productId) {

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		log.info("Will get reviews for product with id={}", productId);

		return Mono.fromCallable(() -> internalGetReviews(productId)).flatMapMany(Flux::fromIterable)
				.log(log.getName(), FINE).subscribeOn(jdbcScheduler);
	}

	public List<Review> internalGetReviews(int productId) {
		log.debug("Getting Reviews for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		List<ReviewEntity> entityList = repository.findByProductId(productId);
		List<Review> list = mapper.entityListToApiList(entityList);
		list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

		log.debug("/reviews response size: {}", list.size());

		return list;
	}

	@Override
	public Mono<Void> deleteReviews(int productId) {

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		return Mono.fromRunnable(() -> internalDeleteReviews(productId)).subscribeOn(jdbcScheduler).then();
	}

	public void internalDeleteReviews(int productId) {
		log.debug("Deleting all Reviews for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());

		repository.deleteAll(repository.findByProductId(productId));
		log.debug("All Reviews Deleted for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());
	}
	
	@Override
	public Mono<Void> publishReviewEvent(@RequestBody Event<Integer, Review> msg) {
	    return Mono.fromCallable(() -> {
	        sendMessage("reviews-out-0", new Event<Integer, Review>(msg.getEventType(), msg.getKey(), msg.getData()));
	        return msg;
	      }).then()
	      .subscribeOn(publishEventScheduler);
	}
	

    private void sendMessage(String bindingName, Event<Integer, Review> event) {
	    log.debug("Sending a Review {} message with ID {} to {}", event.getEventType(), event.getKey(),bindingName);
        Message<Event<Integer, Review>> message = MessageBuilder.withPayload(event)
            .setHeader("partitionKey", event.getKey())
            .build();
        streamBridge.send(bindingName, message);
	    log.debug("{} Review Message with ID {} sent to {}", event.getEventType(), event.getKey(), bindingName);

  }


}
