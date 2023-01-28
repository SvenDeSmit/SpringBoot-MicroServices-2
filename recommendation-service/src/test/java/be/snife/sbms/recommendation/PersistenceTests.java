package be.snife.sbms.recommendation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.recommendation.persistence.RecommendationEntity;
import be.snife.sbms.recommendation.persistence.RecommendationRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
@Slf4j
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private RecommendationRepository repository;

  private RecommendationEntity savedEntity1;
  private RecommendationEntity savedEntity2;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();

    RecommendationEntity entity1 = new RecommendationEntity(1, 1, "a", 9, "c");
	StepVerifier.create(repository.save(entity1)).expectNextMatches(createdEntity -> {
		savedEntity1 = createdEntity;
		return areRecommendationsEqual(savedEntity1, createdEntity);
	}).verifyComplete();
	;   
    
	RecommendationEntity entity2 = new RecommendationEntity(1, 2, "a", 9, "c");
	StepVerifier.create(repository.save(entity2)).expectNextMatches(createdEntity -> {
		savedEntity2 = createdEntity;
		return areRecommendationsEqual(savedEntity2, createdEntity);
	}).verifyComplete();
	;   

	StepVerifier.create(repository.count())
		.expectNext(2L)
	.verifyComplete();
	
  }


  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
    
	StepVerifier.create(repository.save(newEntity)) // create Publisher - Mono
		.expectNextMatches(createdEntity -> {
			log.debug("New entity created [{},{}]",createdEntity.getProductId(),createdEntity.getRecommendationId());
			return newEntity.getProductId() == createdEntity.getProductId();
		})
	.verifyComplete();
	
	StepVerifier.create(repository.findById(newEntity.getId()))
		.expectNextMatches(foundEntity -> areRecommendationsEqual(newEntity, foundEntity))
	.verifyComplete();

	StepVerifier.create(repository.count())
		.expectNext(3L)
	.verifyComplete();
	
  }

  @Test
  void update() {
    savedEntity1.setAuthor("a2");
    
    StepVerifier.create(repository.save(savedEntity1))
		.expectNextMatches(updatedEntity -> updatedEntity.getAuthor().equals("a2"))
	.verifyComplete();

    StepVerifier.create(repository.findById(savedEntity1.getId()))
		.expectNextMatches(foundEntity -> {
			assertEquals(1, foundEntity.getVersion());
			assertEquals("a2", foundEntity.getAuthor());
			return true;
    		//return foundEntity.getVersion() == 1 && foundEntity.getAuthor().equals("a2");
			})
	.verifyComplete();    
    
  }

  @Test
  void delete() {

	StepVerifier.create(repository.delete(savedEntity1))
    .verifyComplete();
    
    StepVerifier.create(repository.existsById(savedEntity1.getId()))
    	.expectNext(false)
    .verifyComplete();

  }

  @Test
  void getByProductId() {
	  
	StepVerifier.create(repository.findByProductId(savedEntity1.getProductId()))
      .expectNextMatches(foundEntity -> areRecommendationsEqual(savedEntity1, foundEntity))
      .expectNextMatches(foundEntity -> areRecommendationsEqual(savedEntity2, foundEntity))
     .verifyComplete();
	  
  }


/* doesn't work with test container  
  @Test
  void duplicateError() {
	  
	RecommendationEntity entity = new RecommendationEntity(1,2, "a", 3, "c");
		
	StepVerifier.create(repository.save(entity))
		.expectError(InvalidInputException.class)
	.verify();
  
  }
*/
  @Test
  void optimisticLockError() {

    // Store the saved entity in two separate entity objects
    RecommendationEntity entity1 = repository.findById(savedEntity1.getId()).block();
    RecommendationEntity entity2 = repository.findById(savedEntity1.getId()).block();

    // Update the entity using the first entity object
    entity1.setAuthor("a2");
    repository.save(entity1).block();
    
    entity2.setAuthor("a3");

    StepVerifier.create(repository.save(entity2))
		.expectError(OptimisticLockingFailureException.class)
	.verify();

    // Get the updated entity from the database and verify its new state
    StepVerifier.create(repository.findById(savedEntity1.getId()))
	  	.expectNextMatches(foundEntity -> {
			assertEquals(1, foundEntity.getVersion());
			assertEquals("a2", foundEntity.getAuthor());
			return true;
	  		})
  	.verifyComplete();
    

  }

  private void assertEqualsRecommendation(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
    assertEquals(expectedEntity.getId(),               actualEntity.getId());
    assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
    assertEquals(expectedEntity.getRecommendationId(), actualEntity.getRecommendationId());
    assertEquals(expectedEntity.getAuthor(),           actualEntity.getAuthor());
    assertEquals(expectedEntity.getRating(),           actualEntity.getRating());
    assertEquals(expectedEntity.getContent(),          actualEntity.getContent());
  }
  
	private boolean areRecommendationsEqual(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
		log.debug("Recommendation: {}, {}",actualEntity.getProductId(),actualEntity.getRecommendationId());
		return (expectedEntity.getId().equals(actualEntity.getId()))
				&& (expectedEntity.getVersion() == actualEntity.getVersion())
				&& (expectedEntity.getProductId() == actualEntity.getProductId())
				&& (expectedEntity.getRecommendationId() == actualEntity.getRecommendationId())
				&& (expectedEntity.getAuthor().equals(actualEntity.getAuthor()))
				&& (expectedEntity.getContent().equals(actualEntity.getContent()))
				&& (expectedEntity.getRating() == actualEntity.getRating())
				
				;
	}
  
}
