package be.snife.sbms.api.core.recommendation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import be.snife.sbms.api.event.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {

	/**
	 * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
	 *
	 * @param productId Id of the product
	 * @return the recommendations of the product
	 */
	@GetMapping(value = "/recommendation", produces = "application/json")
	Flux<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);

	/**
	 * Sample usage, see below.
	 *
	 * curl -X POST $HOST:$PORT/recommendation \ -H "Content-Type: application/json"
	 * --data \
	 * '{"productId":123,"recommendationId":456,"author":"me","rate":5,"content":"yada,
	 * yada, yada"}'
	 *
	 * @param body A JSON representation of the new recommendation
	 * @return A JSON representation of the newly created recommendation
	 */
	@PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	Mono<Recommendation> createRecommendation(@RequestBody Recommendation body);

	/**
	 * Sample usage: "curl -X DELETE $HOST:$PORT/recommendation?productId=1".
	 *
	 * @param productId Id of the product
	 */
	@DeleteMapping(value = "/recommendation")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> deleteRecommendations(@RequestParam(value = "productId", required = true) int productId);
	
	@PostMapping(value = "/recommendation/event", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	Mono<Void> publishRecommendationEvent(@RequestBody Event<Integer, Recommendation> event); 
	

}
