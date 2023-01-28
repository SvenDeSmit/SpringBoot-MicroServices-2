package be.snife.sbms.product;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.ASC;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import be.snife.sbms.product.persistence.ProductEntity;
import be.snife.sbms.product.persistence.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class PersistenceTests extends MongoDbTestBase {

	@Autowired
	private ProductRepository repository;

	private ProductEntity savedEntity;

	@BeforeEach
	void setupDb() {
		repository.deleteAll().block();
		
		// ProductEntity foundEntity = repository.findById(newEntity.getId()).get();

		ProductEntity entity = new ProductEntity(1, "n", 1);

		StepVerifier.create(repository.save(entity)).expectNextMatches(createdEntity -> {
			savedEntity = createdEntity;
			return areProductEqual(savedEntity, createdEntity);
		}).verifyComplete();
		;
		
		//savedEntity = repository.save(entity).block();
		//assertEqualsProduct(entity, savedEntity);
	}


	@Test
	void create() {

		ProductEntity newEntity = new ProductEntity(2, "n", 2);
		//repository.save(newEntity);

		// ProductEntity foundEntity = repository.findById(newEntity.getId()).get();

		// Alternative use block statement to get result
		// ProductEntity foundEntity = repository.findById(newEntity.getId()).block();
		// assertEqualsProduct(newEntity, foundEntity);
		// assertEquals(2, repository.count());

		// Use StepVerifier
		StepVerifier
			.create(repository.save(newEntity)) // create Publisher - Flux
			.expectNextMatches(createdEntity -> {
				log.debug("New entity created [{},{}]",createdEntity.getId(),createdEntity.getName());
				return newEntity.getProductId() == createdEntity.getProductId();
			})
			.verifyComplete();

		StepVerifier
			.create(repository.findById(newEntity.getId()))
			.expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
			.verifyComplete();

		StepVerifier
			.create(repository.count())
			.expectNext(2L)
			.verifyComplete();

	}


	@Test
	void update() {
		savedEntity.setName("n2");
		
		//repository.save(savedEntity);

		// ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
		//ProductEntity foundEntity = repository.findById(savedEntity.getId()).block();
		//assertEquals(1, (long) foundEntity.getVersion());
		//assertEquals("n2", foundEntity.getName());
		
	    StepVerifier
	    	.create(repository.save(savedEntity))
	    	.expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
	    	.verifyComplete();

	    StepVerifier
	    	.create(repository.findById(savedEntity.getId()))
	    	.expectNextMatches(foundEntity ->
	        	foundEntity.getVersion() == 1 && foundEntity.getName().equals("n2"))
	    	.verifyComplete();
		
	}

	@Test
	void delete() {
			
		//repository.delete(savedEntity);
		//assertFalse(repository.existsById(savedEntity.getId()));
		
	    StepVerifier
	    	.create(repository.delete(savedEntity))
	    	.verifyComplete();
	    
	    StepVerifier
	    	.create(repository.existsById(savedEntity.getId()))
	    	.expectNext(false)
	    	.verifyComplete();
		
	}

	@Test
	void getByProductId() {

		//Mono<ProductEntity> entity = repository.findByProductId(savedEntity.getProductId());
		//assertTrue(entity.isPresent());
		//assertEqualsProduct(savedEntity, entity.get());
		
	    StepVerifier
	    	.create(repository.findByProductId(savedEntity.getProductId()))
	      .expectNextMatches(foundEntity -> areProductEqual(savedEntity, foundEntity))
	      .verifyComplete();
		
	}

	@Test
	void duplicateError() {
	//	assertThrows(DuplicateKeyException.class, () -> {
	//		ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
	//		repository.save(entity);
	//	});
		
	    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
	    StepVerifier
	    	.create(repository.save(entity))
	    	.expectError(DuplicateKeyException.class)
	    	.verify();

	}

	
	@Test
	void optimisticLockError() {

		// Store the saved entity in two separate entity objects
		ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
		ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

		// Update the entity using the first entity object
		entity1.setName("n1");
		repository.save(entity1).block();

		// Update the entity using the second entity object.
		// This should fail since the second entity now holds an old version number,
		// i.e. an Optimistic Lock Error
		//assertThrows(OptimisticLockingFailureException.class, () -> {
		//	entity2.setName("n2");
		//	repository.save(entity2);
		//});

		// Get the updated entity from the database and verify its new sate
		//ProductEntity updatedEntity = repository.findById(savedEntity.getId()).get();
		//assertEquals(1, (int) updatedEntity.getVersion());
		//assertEquals("n1", updatedEntity.getName());
		
	    //  Update the entity using the second entity object.
	    // This should fail since the second entity now holds a old version number, i.e. a Optimistic Lock Error
	    StepVerifier
	    	.create(repository.save(entity2))
	    	.expectError(OptimisticLockingFailureException.class)
	    	.verify();

	    // Get the updated entity from the database and verify its new sate
	    StepVerifier.create(repository.findById(savedEntity.getId()))
	      	.expectNextMatches(foundEntity ->
	      		foundEntity.getVersion() == 1 && foundEntity.getName().equals("n1"))
	      	.verifyComplete();
		
	}
	

	/*
	@Test
	void paging() {

		repository.deleteAll();

		List<ProductEntity> newProducts = rangeClosed(1001, 1010).mapToObj(i -> new ProductEntity(i, "name " + i, i))
				.collect(Collectors.toList());
		repository.saveAll(newProducts);

		Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
		nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
		nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
		nextPage = testNextPage(nextPage, "[1009, 1010]", false);
	}
	

	private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
		Page<ProductEntity> productPage = repository.findAll(nextPage);
		assertEquals(expectedProductIds,
				productPage.getContent().stream().map(p -> p.getProductId()).collect(Collectors.toList()).toString());
		assertEquals(expectsNextPage, productPage.hasNext());
		return productPage.nextPageable();
	}
*/
	private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
		assertEquals(expectedEntity.getId(), actualEntity.getId());
		assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
		assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
		assertEquals(expectedEntity.getName(), actualEntity.getName());
		assertEquals(expectedEntity.getWeight(), actualEntity.getWeight());
	}

	private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
		return (expectedEntity.getId().equals(actualEntity.getId()))
				&& (expectedEntity.getVersion() == actualEntity.getVersion())
				&& (expectedEntity.getProductId() == actualEntity.getProductId())
				&& (expectedEntity.getName().equals(actualEntity.getName()))
				&& (expectedEntity.getWeight() == actualEntity.getWeight());
	}

}
