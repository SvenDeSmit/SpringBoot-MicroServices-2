package be.snife.sbms.product.services;

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

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

	// private static final Logger LOG =
	// LoggerFactory.getLogger(ProductServiceImpl.class);

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
	public Product createProduct(Product body) {
		try {
			ProductEntity entity = mapper.apiToEntity(body);
			ProductEntity newEntity = repository.save(entity);

			log.debug("createProduct: entity created for productId: {}", body.getProductId());
			return mapper.entityToApi(newEntity);

		} catch (DuplicateKeyException dke) {
			throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
		}
	}

	@Override
	public Product getProduct(int productId) {
		log.debug("Getting Product with productId = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		ProductEntity entity = repository.findByProductId(productId)
				.orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));

		Product response = mapper.entityToApi(entity);
		response.setServiceAddress(serviceUtil.getServiceAddress());

		log.debug("getProduct: found productId: {}", response.getProductId());

		return response;
	}

	@Override
	public void deleteProduct(int productId) {
		log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
		repository.findByProductId(productId).ifPresent(e -> repository.delete(e));
	}

}
