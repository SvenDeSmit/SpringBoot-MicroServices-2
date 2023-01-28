package be.snife.sbms.product.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, String> {
	  Mono<ProductEntity> findByProductId(int productId);
	}

/*
public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String> {
  Optional<ProductEntity> findByProductId(int productId);
}
*/
