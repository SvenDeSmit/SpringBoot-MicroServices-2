package be.snife.sbms.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

import java.util.List;
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
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.event.Event.Type;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.recommendation.persistence.RecommendationEntity;
import be.snife.sbms.recommendation.persistence.RecommendationRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Slf4j
class RecommendationServiceApplicationTests extends MongoDbTestBase {

	@Autowired
	private WebTestClient client;

	@Autowired
	private RecommendationRepository repository;
	
	@Autowired
	@Qualifier("messageProcessor")
	private Consumer<Event<Integer, Recommendation>> messageProcessor;
	

	@BeforeEach
	void setupDb() {
		repository.deleteAll().block();
		RecommendationEntity recommendation1 = new RecommendationEntity(1, 1, "Author 1",1,"Content 1");
		repository.save(recommendation1).block();
		RecommendationEntity recommendation2 = new RecommendationEntity(1, 2, "Author 2",2,"Content 2");		
		repository.save(recommendation2).block();
		RecommendationEntity recommendation3 = new RecommendationEntity(1, 3, "Author 3",3,"Content 3");		
		repository.save(recommendation3).block();
	}

	@Test
	void getRecommendationsByProductId() {

		int productId = 1;
		
		StepVerifier
		.create(repository.count())
			.expectNext(3L)
		.verifyComplete();
		
		WebTestClient.BodyContentSpec body = getAndVerifyRecommendationsByProductId(productId, OK);
		assertThat(body.jsonPath("$.length()").isEqualTo(3));
		assertThat(body.jsonPath("$[*][?(@.recommendationId == 1)].author").isEqualTo("Author 1"));
		assertThat(body.jsonPath("$[*][?(@.recommendationId == 2)].author").isEqualTo("Author 2"));
		assertThat(body.jsonPath("$[*][?(@.recommendationId == 3)].author").isEqualTo("Author 3"));
		
	}
		

	@Test
	void getRecommendationsMissingParameter() {

		getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
			.jsonPath("$.path").isEqualTo("/recommendation")
			.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	void getRecommendationsInvalidParameter() {
		
		getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
			.jsonPath("$.path").isEqualTo("/recommendation")
			.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	void getRecommendationsNotFound() {

		getAndVerifyRecommendationsByProductId("?productId=113", OK)
			.jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	void getRecommendationsInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/recommendation").jsonPath("$.message")
				.isEqualTo("Invalid productId: " + productIdInvalid);
	}

	
	@Test
	void createRecommendation() {
		log.debug("Running createRecommendation() ...");

		int productId = 2;
		Recommendation rec = new Recommendation(productId, 1, "Author 2-1",21,"Content 2-1","AD1");
		
		assertEquals(0,repository.findByProductId(productId).count().block());

		sendCreateRecommendationEvent(rec);

		assertEquals(1,repository.findByProductId(productId).count().block());
		
		WebTestClient.BodyContentSpec body = getAndVerifyRecommendationsByProductId(productId, OK);
		assertThat(body.jsonPath("$.length()").isEqualTo(1));
		assertThat(body.jsonPath("$[*][?(@.recommendationId == 1)].author").isEqualTo("Author 2-1"));
		assertThat(body.jsonPath("$[*][?(@.recommendationId == 1)].productId").isEqualTo(2));
		
	}

	/* doesn't work with test container  
	@Test
	void duplicateError() {

		int productId = 1;
		Recommendation rec = new Recommendation(productId, 1, "Author 1-1",11,"Content 1-1","AD1");

		assertEquals(3,repository.findByProductId(productId).count().block());

		sendCreateRecommendationEvent(rec);

		assertEquals(3,repository.findByProductId(productId).count().block());
		
		InvalidInputException thrown = assertThrows(InvalidInputException.class, () -> sendCreateRecommendationEvent(rec),
				"Expected a InvalidInputException here!");
		assertEquals("Duplicate key, Product Id: " + productId, thrown.getMessage());
//		ResponseSpec resspec = postAndVerifyProduct(dupprod, HttpStatus.UNPROCESSABLE_ENTITY);
//		resspec.expectBody().jsonPath("$.error").isEqualTo("Unprocessable Entity").jsonPath("message")
//				.isEqualTo("Duplicate key, Product Id: 1");

	} */
	

	@Test
	void deleteRecommendations() {

		int productId = 1;

		assertEquals(3,repository.findByProductId(productId).count().block());

		sendDeleteRecommendationEvent(productId);

		assertEquals(0,repository.findByProductId(productId).count().block());
		
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId,HttpStatus expectedStatus) {
		return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery,
			HttpStatus expectedStatus) {
		return client.get().uri("/recommendation" + productIdQuery).accept(APPLICATION_JSON).exchange()
				.expectStatus().isEqualTo(expectedStatus).expectHeader().contentType(APPLICATION_JSON).expectBody();
	}

	private WebTestClient.BodyContentSpec postAndVerifyRecommendation(Recommendation rec,HttpStatus expectedStatus) {
		return client.post().uri("/recommendation").body(just(rec), Recommendation.class)
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(expectedStatus).expectHeader()
				.contentType(APPLICATION_JSON).expectBody();
	}

	
	private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(int productId,HttpStatus expectedStatus) {
		return client.delete().uri("/recommendation?productId=" + productId).accept(APPLICATION_JSON).exchange()
				.expectStatus().isEqualTo(expectedStatus).expectBody();
	}

	
	private void sendCreateRecommendationEvent(Recommendation recommendation) {
		Event<Integer, Recommendation> event = new Event<>(Type.CREATE, recommendation.getProductId(), recommendation);
		messageProcessor.accept(event);
	}

	private void sendDeleteRecommendationEvent(int productId) {
		Event<Integer, Recommendation> event = new Event(Type.DELETE, productId, null);
		messageProcessor.accept(event);
	}

	private void publishRecommendationEvent(Event<Integer, Recommendation> event) {
		messageProcessor.accept(event);
	}
	
}