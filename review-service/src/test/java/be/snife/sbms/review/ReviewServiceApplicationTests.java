package be.snife.sbms.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.event.Event.Type;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.review.persistence.ReviewEntity;
import be.snife.sbms.review.persistence.ReviewRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Slf4j
class ReviewServiceApplicationTests extends MySqlTestBase {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ReviewRepository repository;

	@BeforeEach
	void setupDb() {
		repository.deleteAll();
		ReviewEntity re1 = new ReviewEntity(1, 1, "Author 1","Subject 1","Content 1");
		repository.save(re1);
		ReviewEntity re2 = new ReviewEntity(1, 2, "Author 2","Subject 2","Content 2");
		repository.save(re2);
		ReviewEntity re3 = new ReviewEntity(1, 3, "Author 3","Subject 3","Content 3");
		repository.save(re3);
	}

	@Autowired
	@Qualifier("messageProcessor")
	private Consumer<Event<Integer, Review>> messageProcessor;

	@Test
	void getReviewsByProductId() {

		int productId = 1;

		assertEquals(3, repository.findByProductId(productId).size());
		
		WebTestClient.BodyContentSpec body = getAndVerifyReviewsByProductId(productId, OK);
		assertThat(body.jsonPath("$.length()").isEqualTo(3));
		assertThat(body.jsonPath("$[*][?(@.reviewId == 1)].author").isEqualTo("Author 1"));
		assertThat(body.jsonPath("$[*][?(@.reviewId == 2)].author").isEqualTo("Author 2"));
		assertThat(body.jsonPath("$[*][?(@.reviewId == 3)].author").isEqualTo("Author 3"));
		

		getAndVerifyReviewsByProductId(productId, OK).jsonPath("$.length()").isEqualTo(3).jsonPath("$[2].productId")
				.isEqualTo(productId).jsonPath("$[2].reviewId").isEqualTo(3);
	}

	@Test
	void getReviewsMissingParameter() {

		getAndVerifyReviewsByProductId("", BAD_REQUEST).jsonPath("$.path").isEqualTo("/review").jsonPath("$.message")
				.isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	void getReviewsInvalidParameter() {

		getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST).jsonPath("$.path").isEqualTo("/review")
				.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	void getReviewsNotFound() {

		getAndVerifyReviewsByProductId("?productId=213", OK).jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	void getReviewsInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY).jsonPath("$.path")
				.isEqualTo("/review").jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}

	
	@Test
	void createReview() {
		log.debug("Running createReview() ...");

		int productId = 2;
		Review rev = new Review(productId, 1, "Author 2-1","Subject 2-1","Content 2-1","AD1");
		
		assertEquals(0,repository.findByProductId(productId).size());

		sendCreateReviewEvent(rev);

		assertEquals(1,repository.findByProductId(productId).size());
		
		WebTestClient.BodyContentSpec body = getAndVerifyReviewsByProductId(productId, OK);
		assertThat(body.jsonPath("$.length()").isEqualTo(1));
		assertThat(body.jsonPath("$[*][?(@.reviewId == 1)].author").isEqualTo("Author 2-1"));
		assertThat(body.jsonPath("$[*][?(@.reviewId == 1)].productId").isEqualTo(2));
		
	}	
	
	@Test
	void duplicateError() {

		int productId = 1;
		int reviewId = 1;
		Review rev = new Review(productId, 1, "Author 2-1","Subject 2-1","Content 2-1","AD1");

		assertEquals(3, repository.findByProductId(productId).size());

		InvalidInputException thrown = assertThrows(InvalidInputException.class, () -> sendCreateReviewEvent(rev),
				"Expected a InvalidInputException here!");
		assertEquals("Duplicate key, Product Id: 1, Review Id:1", thrown.getMessage());

		assertEquals(3, repository.findByProductId(productId).size());

	}

	@Test
	void deleteReviews() {

		int productId = 1;
		int reviewId = 1;
		
		assertEquals(3,repository.findByProductId(productId).size());

		sendDeleteReviewEvent(productId);

		assertEquals(0,repository.findByProductId(productId).size());
		
	}

	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
		return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery,
			HttpStatus expectedStatus) {
		return client.get().uri("/review" + productIdQuery).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(expectedStatus).expectHeader().contentType(APPLICATION_JSON).expectBody();
	}

	private WebTestClient.BodyContentSpec postAndVerifyReview(int productId, int reviewId, HttpStatus expectedStatus) {
		Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId,
				"Content " + reviewId, "SA");
		return client.post().uri("/review").body(just(review), Review.class).accept(APPLICATION_JSON).exchange()
				.expectStatus().isEqualTo(expectedStatus).expectHeader().contentType(APPLICATION_JSON).expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
		return client.delete().uri("/review?productId=" + productId).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(expectedStatus).expectBody();
	}
	
	private void sendCreateReviewEvent(Review review) {
		Event<Integer, Review> event = new Event<>(Type.CREATE, review.getProductId(), review);
		messageProcessor.accept(event);
	}

	private void sendDeleteReviewEvent(int reviewId) {
		Event<Integer, Review> event = new Event(Type.DELETE, reviewId, null);
		messageProcessor.accept(event);
	}

	private void publishReviewEvent(Event<Integer, Review> event) {
		messageProcessor.accept(event);
	}
	
}