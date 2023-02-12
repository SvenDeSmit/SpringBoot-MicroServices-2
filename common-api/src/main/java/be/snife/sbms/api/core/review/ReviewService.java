package be.snife.sbms.api.core.review;

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

public interface ReviewService {

	/**
	 * Sample usage: "curl $HOST:$PORT/review?productId=1".
	 *
	 * @param productId Id of the product
	 * @return the reviews of the product
	 */
	@GetMapping(value = "/review", produces = "application/json")
	Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

	/**
	 * Sample usage, see below.
	 *
	 * curl -X POST $HOST:$PORT/review \ -H "Content-Type: application/json" --data
	 * \ '{"productId":123,"reviewId":456,"author":"me","subject":"yada, yada,
	 * yada","content":"yada, yada, yada"}'
	 *
	 * @param body A JSON representation of the new review
	 * @return A JSON representation of the newly created review
	 */
	@PostMapping(value = "/review", consumes = "application/json", produces = "application/json")
	Mono<Review> createReview(@RequestBody Review body);

	/**
	 * Sample usage: "curl -X DELETE $HOST:$PORT/review?productId=1".
	 *
	 * @param productId Id of the product
	 */
	@DeleteMapping(value = "/review")
	Mono<Void> deleteReviews(@RequestParam(value = "productId", required = true) int productId);

	@PostMapping(value = "/review/event", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<Void> publishReviewEvent(@RequestBody Event<Integer, Review> event);
	  
}
