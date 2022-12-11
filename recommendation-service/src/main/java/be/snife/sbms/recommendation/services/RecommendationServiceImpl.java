package be.snife.sbms.recommendation.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;


import lombok.extern.slf4j.Slf4j;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.core.recommendation.RecommendationService;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.recommendation.persistence.RecommendationEntity;
import be.snife.sbms.recommendation.persistence.RecommendationRepository;
import be.snife.sbms.util.http.ServiceUtil;

@RestController
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

	private final ServiceUtil serviceUtil;
	private final RecommendationRepository repository;
	private final RecommendationMapper mapper;

	@Autowired
	public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper,
			ServiceUtil serviceUtil) {
		this.serviceUtil = serviceUtil;
		this.mapper = mapper;
		this.repository = repository;
	}

	@Override
	public Recommendation createRecommendation(Recommendation body) {
		try {
			RecommendationEntity entity = mapper.apiToEntity(body);
			RecommendationEntity newEntity = repository.save(entity);

			log.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(),
					body.getRecommendationId());
			return mapper.entityToApi(newEntity);

		} catch (DuplicateKeyException dke) {
			throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:"
					+ body.getRecommendationId());
		}
	}

	@Override
	public List<Recommendation> getRecommendations(int productId) {
		log.debug("Getting Recommendations for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		if (productId == 113) {
			log.debug("No recommendations found for productId: {}", productId);
			return new ArrayList<>();
		}

	    List<RecommendationEntity> entityList = repository.findByProductId(productId);
	    List<Recommendation> list = mapper.entityListToApiList(entityList);
	    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

		log.debug("/recommendation response size: {}", list.size());

		return list;
	}

	@Override
	public void deleteRecommendations(int productId) {
		log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}",
				productId);
		repository.deleteAll(repository.findByProductId(productId));
	}
}
