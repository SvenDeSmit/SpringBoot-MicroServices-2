package be.snife.sbms.recommendation.persistence;

import java.util.List;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {
  Flux<RecommendationEntity> findByProductId(int productId);
}
