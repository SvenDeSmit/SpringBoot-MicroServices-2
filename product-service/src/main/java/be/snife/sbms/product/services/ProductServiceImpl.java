package be.snife.sbms.product.services;

import static java.util.logging.Level.FINE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.product.ProductService;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.api.exceptions.NotFoundException;
import be.snife.sbms.product.persistence.ProductEntity;
import be.snife.sbms.product.persistence.ProductRepository;
import be.snife.sbms.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

	private final ServiceUtil serviceUtil;
	private final ProductRepository repository;
	private final ProductMapper mapper;

	@Autowired
	public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
	}

	@Override
	public Mono<Product> createProduct(Product body) {
		if (body.getProductId() < 1) {
			throw new InvalidInputException("Invalid productId: " + body.getProductId());
		}

		log.debug("Creating Product entity for productId {} ...", body.getProductId());
		ProductEntity entity = mapper.apiToEntity(body);
		Mono<Product> newEntity = repository.save(entity)
				//.log(log.getName(), FINE)
				.onErrorMap(DuplicateKeyException.class,
						ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
				.map(ent -> mapper.entityToApi(ent));

		return newEntity;
	}

	@Override
	public Mono<Product> getProduct(int productId) {
		log.debug("Getting Product with productId = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		log.debug("Getting Product entity for productId {} ...", productId);
		
		Mono<Product> prod = repository.findByProductId(productId)
				.switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
				//.log(log.getName(), FINE)
				.map(p -> mapper.entityToApi(p))
				.map(e -> setServiceAddress(e));
		
		return prod;
	}

	@Override
	public Mono<Void> deleteProduct(int productId) {
		log.debug("Deleting Product entity with productId: {} ...", productId);

		return repository.findByProductId(productId).log(log.getName(), FINE).map(p -> repository.delete(p)).flatMap(p -> p);
		//repository.findByProductId(productId).log(log.getName(), FINE).map(p -> repository.delete(p)).flatMap(p -> p);
		// repository.findByProductId(productId).ifPresent(p -> deleteProduct(p));
		//repository.findByProductId(productId).;
		//log.debug("Product name: {} ...", prodent.getName());
		//.ifPresent(e -> repository.delete(e));
	}

	private Product setServiceAddress(Product e) {
		e.setServiceAddress(serviceUtil.getServiceAddress());
		return e;
	}
	
	private Mono<Void> deleteProduct(ProductEntity p) {
		log.debug("Deleting Product entity with product name: {} ...", p.getName());
		return repository.delete(p);
	}

}
