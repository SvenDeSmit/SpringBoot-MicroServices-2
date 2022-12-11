package be.snife.sbms.review.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.api.core.review.ReviewService;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.review.persistence.ReviewEntity;
import be.snife.sbms.review.persistence.ReviewRepository;
import be.snife.sbms.util.http.ServiceUtil;

@RestController
@Slf4j
public class ReviewServiceImpl implements ReviewService {

	private final ServiceUtil serviceUtil;
	private final ReviewRepository repository;
	private final ReviewMapper mapper;

	@Autowired
	public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
	}

	@Override
	public Review createReview(Review body) {
		log.debug("Creating Review for Product with ID = {} and ReviewID {} on {}", body.getProductId(), body.getReviewId(), serviceUtil.getServiceAddress());

		try {
			ReviewEntity entity = mapper.apiToEntity(body);
			ReviewEntity newEntity = repository.save(entity);

			log.debug("Review for Product with ID = {} and ReviewID {} is stored", body.getProductId(), body.getReviewId());
			Review res = mapper.entityToApi(newEntity);
			res.setServiceAddress(serviceUtil.getServiceAddress());
			
			return res;

		} catch (DataIntegrityViolationException dive) {
			throw new InvalidInputException(
					"Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
		}
	}

	@Override
	public List<Review> getReviews(int productId) {
		log.debug("Getting Reviews for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());

		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

	    List<ReviewEntity> entityList = repository.findByProductId(productId);
	    List<Review> list = mapper.entityListToApiList(entityList);
	    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

		log.debug("/reviews response size: {}", list.size());

		return list;
	}

	@Override
	public void deleteReviews(int productId) {
		log.debug("Deleting all Reviews for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());

		repository.deleteAll(repository.findByProductId(productId));
		log.debug("All Reviews Deleted for Product with ID = {} on {}", productId, serviceUtil.getServiceAddress());
	}

}
