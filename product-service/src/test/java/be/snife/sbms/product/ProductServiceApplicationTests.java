package be.snife.sbms.product;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.event.Event.Type;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.product.persistence.ProductEntity;
import be.snife.sbms.product.persistence.ProductRepository;
import be.snife.sbms.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//@SpringBootTest
@AutoConfigureWebTestClient
@Slf4j
class ProductServiceApplicationTests extends MongoDbTestBase {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ProductRepository repository;

	@Autowired
	@Qualifier("messageProcessor")
	private Consumer<Event<Integer, Product>> messageProcessor;

	@BeforeEach
	void setupDb() {
		repository.deleteAll().block();
		repository.save(new ProductEntity(1, "a", 10)).block();
		repository.save(new ProductEntity(999, "delete-test", 999)).block();
	}

	@Test
	void getProductById() {
		log.debug("Running getProductById() ...");

		int productId = 1;

		assertNotNull(repository.findByProductId(productId).block());
		assertEquals(2, (long) repository.count().block());

		// Product prod = getAndVerifyProduct(productId, OK);
		ResponseSpec dbresspec = getAndVerifyProduct(productId, OK);
		Product prod = dbresspec.expectBody(Product.class).returnResult().getResponseBody();

		log.debug("Product found with ID = {}", prod.getProductId());
		assertEquals(1, prod.getProductId());
		assertEquals("a", prod.getName());
		assertEquals(10, prod.getWeight());

	}

	@Test
	void getProductInvalidParameterString() {
		ResponseSpec resspec = getAndVerifyProduct("/no-int", HttpStatus.BAD_REQUEST);
		resspec.expectBody().jsonPath("$.error").isEqualTo("Bad Request").jsonPath("message")
				.isEqualTo("Type mismatch.");
	}

	@Test
	void getProductNotFound() {
		int productIdNotFound = 13;
		ResponseSpec resspec = getAndVerifyProduct(productIdNotFound, HttpStatus.NOT_FOUND);
		resspec.expectBody().jsonPath("$.error").isEqualTo("Not Found").jsonPath("message")
				.isEqualTo("No product found for productId: 13");
	}

	@Test
	void getProductInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		ResponseSpec resspec = getAndVerifyProduct(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY);
		resspec.expectBody().jsonPath("$.error").isEqualTo("Unprocessable Entity").jsonPath("message")
				.isEqualTo("Invalid productId: -1");

	}

	@Test
	void createProduct() {
		log.debug("Running createProduct() ...");
		int productId = 10;
		Product newprod = new Product(10, "Product 10", 100, "Dummy address");

		assertNull(repository.findByProductId(productId).block());

		sendCreateProductEvent(newprod);

		assertNotNull(repository.findByProductId(productId).block());

		ResponseSpec dbresspec = getAndVerifyProduct(productId, OK);
		Product dbprod = dbresspec.expectBody(Product.class).returnResult().getResponseBody();
		log.debug("New Product found with ID = {}", dbprod.getProductId());
		assertEquals(10, dbprod.getProductId());
		assertEquals("Product 10", dbprod.getName());
		assertEquals(100, dbprod.getWeight());
	}

	/* doesn't work with test container  
	@Test
	void duplicateError() {

		int productId = 1;
		Product dupprod = new Product(1, "Product 1", 100, "Dummy address");
		assertNotNull(repository.findByProductId(productId).block());

		// sendCreateProductEvent(dupprod);

		InvalidInputException thrown = assertThrows(InvalidInputException.class, () -> sendCreateProductEvent(dupprod),
				"Expected a InvalidInputException here!");
		assertEquals("Duplicate key, Product Id: " + productId, thrown.getMessage());
//		ResponseSpec resspec = postAndVerifyProduct(dupprod, HttpStatus.UNPROCESSABLE_ENTITY);
//		resspec.expectBody().jsonPath("$.error").isEqualTo("Unprocessable Entity").jsonPath("message")
//				.isEqualTo("Duplicate key, Product Id: 1");

	} */

	@Test
	void deleteProduct() {

		int productId = 999;
		assertNotNull(repository.findByProductId(productId).block());
		assertEquals(2, (long) repository.count().block());

		sendDeleteProductEvent(productId);

		assertEquals(1, (long) repository.count().block());
		assertNull(repository.findByProductId(productId).block());

	}

	@Test
	void publishCreateProductEvent() {

		int productId = 100;
		Product newprod = new Product(100, "Product 100", 100, "Dummy address");
		Event<Integer, Product> event = new Event<>(Type.CREATE, newprod.getProductId(), newprod);

		assertNull(repository.findByProductId(productId).block());
		assertEquals(2, (long) repository.count().block());
		
		publishProductEvent(event);

		assertEquals(3, (long) repository.count().block());
		assertNotNull(repository.findByProductId(productId).block());
	}
	
	private ResponseSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return getAndVerifyProduct("/" + productId, expectedStatus);
	}

	private ResponseSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
		return client.get().uri("/product" + productIdPath).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(expectedStatus).expectHeader().contentType(APPLICATION_JSON);
	}

	private ResponseSpec postAndVerifyProduct(Product prod, HttpStatus expectedStatus) {

		return client.post().uri("/product").body(just(prod), Product.class).accept(APPLICATION_JSON).exchange()
				.expectStatus().isEqualTo(expectedStatus).expectHeader().contentType(APPLICATION_JSON);
	}

	private ResponseSpec deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {

		return client.delete().uri("/product/" + productId).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(expectedStatus);
	}

	private void sendCreateProductEvent(Product product) {
		Event<Integer, Product> event = new Event<>(Type.CREATE, product.getProductId(), product);
		messageProcessor.accept(event);
	}

	private void sendDeleteProductEvent(int productId) {
		Event<Integer, Product> event = new Event(Type.DELETE, productId, null);
		messageProcessor.accept(event);
	}

	private void publishProductEvent(Event<Integer, Product> event) {
		messageProcessor.accept(event);
	}
	
	/*
	 * @Test void getProductById() {
	 * 
	 * int productId = 1;
	 * 
	 * postAndVerifyProduct(productId, OK);
	 * 
	 * assertTrue(repository.findByProductId(productId).isPresent());
	 * 
	 * getAndVerifyProduct(productId,
	 * OK).jsonPath("$.productId").isEqualTo(productId); }
	 * 
	 * @Test void duplicateError() {
	 * 
	 * int productId = 1;
	 * 
	 * postAndVerifyProduct(productId, OK);
	 * 
	 * assertTrue(repository.findByProductId(productId).isPresent());
	 * 
	 * postAndVerifyProduct(productId,
	 * UNPROCESSABLE_ENTITY).jsonPath("$.path").isEqualTo("/product")
	 * .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
	 * }
	 * 
	 * @Test void deleteProduct() {
	 * 
	 * int productId = 1;
	 * 
	 * postAndVerifyProduct(productId, OK);
	 * assertTrue(repository.findByProductId(productId).isPresent());
	 * 
	 * deleteAndVerifyProduct(productId, OK);
	 * assertFalse(repository.findByProductId(productId).isPresent());
	 * 
	 * deleteAndVerifyProduct(productId, OK); }
	 * 
	 * @Test void getProductInvalidParameterString() {
	 * 
	 * getAndVerifyProduct("/no-integer",
	 * BAD_REQUEST).jsonPath("$.path").isEqualTo("/product/no-integer")
	 * .jsonPath("$.message").isEqualTo("Type mismatch."); }
	 * 
	 * @Test void getProductNotFound() {
	 * 
	 * int productIdNotFound = 13; getAndVerifyProduct(productIdNotFound,
	 * NOT_FOUND).jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
	 * .jsonPath("$.message").isEqualTo("No product found for productId: " +
	 * productIdNotFound); }
	 * 
	 * @Test void getProductInvalidParameterNegativeValue() {
	 * 
	 * int productIdInvalid = -1;
	 * 
	 * getAndVerifyProduct(productIdInvalid,
	 * UNPROCESSABLE_ENTITY).jsonPath("$.path") .isEqualTo("/product/" +
	 * productIdInvalid).jsonPath("$.message") .isEqualTo("Invalid productId: " +
	 * productIdInvalid); }
	 * 
	 * private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId,
	 * HttpStatus expectedStatus) { return getAndVerifyProduct("/" + productId,
	 * expectedStatus); }
	 * 
	 * private WebTestClient.BodyContentSpec getAndVerifyProduct(String
	 * productIdPath, HttpStatus expectedStatus) { return
	 * client.get().uri("/product" +
	 * productIdPath).accept(APPLICATION_JSON).exchange().expectStatus()
	 * .isEqualTo(expectedStatus).expectHeader().contentType(APPLICATION_JSON).
	 * expectBody(); }
	 * 
	 * private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId,
	 * HttpStatus expectedStatus) { Product product = new Product(productId, "Name "
	 * + productId, productId, "SA"); return
	 * client.post().uri("/product").body(just(product),
	 * Product.class).accept(APPLICATION_JSON).exchange()
	 * .expectStatus().isEqualTo(expectedStatus).expectHeader().contentType(
	 * APPLICATION_JSON).expectBody(); }
	 * 
	 * private WebTestClient.BodyContentSpec deleteAndVerifyProduct(int productId,
	 * HttpStatus expectedStatus) { return client.delete().uri("/product/" +
	 * productId).accept(APPLICATION_JSON).exchange().expectStatus()
	 * .isEqualTo(expectedStatus).expectBody(); }
	 */
}
