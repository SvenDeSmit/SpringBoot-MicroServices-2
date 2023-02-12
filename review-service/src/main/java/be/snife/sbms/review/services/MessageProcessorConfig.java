package be.snife.sbms.review.services;

import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.api.core.review.ReviewService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MessageProcessorConfig {

	private final ReviewService reviewService;

	@Autowired
	public MessageProcessorConfig(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@Bean
	public Consumer<Event<Integer, Review>> messageProcessor() {
		return event -> {
			log.info("Review Event received at {}...", event.getEventCreatedAt());
			log.debug("[{} , {}, {}, {}]", event.getEventCreatedAt(), event.getEventType(), event.getKey(),
					event.getData());

			switch (event.getEventType()) {

			case CREATE:
				Review review = event.getData();
				log.info("Creating Review with ID: {} for Product with ID {}", review.getReviewId(), review.getProductId());
				reviewService.createReview(review).block();
				break;

			case DELETE:
				int productId = event.getKey();
				log.info("Deleting Review with ID: {}", productId);
				reviewService.deleteReviews(productId).block();
				break;

			default:
				String errorMessage = "Incorrect event type: " + event.getEventType()
						+ ", expected a CREATE or DELETE event";
				log.warn(errorMessage);
				throw new EventProcessingException(errorMessage);
			}

			log.info("Message processing done!");

		};
	}
}
