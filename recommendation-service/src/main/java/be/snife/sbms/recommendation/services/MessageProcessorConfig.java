package be.snife.sbms.recommendation.services;

import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.core.recommendation.RecommendationService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MessageProcessorConfig {

	private final RecommendationService recommendationService;

	@Autowired
	public MessageProcessorConfig(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@Bean
	public Consumer<Event<Integer, Recommendation>> messageProcessor() {
		return event -> {
			log.info("Recommendation Event received at {}...", event.getEventCreatedAt());
			log.debug("[{} , {}, {}, {}]", event.getEventCreatedAt(), event.getEventType(), event.getKey(),
					event.getData());

			switch (event.getEventType()) {

			case CREATE:
				Recommendation recommendation = event.getData();
				log.info("Creating Recommendation with ID: {} for Product with ID {}", recommendation.getRecommendationId(), recommendation.getProductId());
				recommendationService.createRecommendation(recommendation).block();
				break;

			case DELETE:
				int recommendationId = event.getKey();
				log.info("Deleting Recommendation with ID: {}", recommendationId);
				recommendationService.deleteRecommendations(recommendationId).block();
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
