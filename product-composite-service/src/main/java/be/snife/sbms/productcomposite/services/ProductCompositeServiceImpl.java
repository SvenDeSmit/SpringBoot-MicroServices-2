package be.snife.sbms.productcomposite.services;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import be.snife.sbms.api.composite.product.*;
import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.api.exceptions.NotFoundException;
import be.snife.sbms.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ProductCompositeServiceImpl implements ProductCompositeService {

	private final ServiceUtil serviceUtil;
	private ProductCompositeIntegration integration;

	@Autowired
	public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {

		this.serviceUtil = serviceUtil;
		this.integration = integration;
	}

	@Override
	public Mono<Void> createProduct(ProductAggregate body) {

		try {

		    List<Mono> monoList = new ArrayList<>(); // keeps results, but not used! thrash bin

			log.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

			Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
			log.debug("Product = "+product);
			monoList.add(integration.createProduct(product));

			if (body.getRecommendations() != null) {
				body.getRecommendations().forEach(r -> {
					Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(),
							r.getAuthor(), r.getRate(), r.getContent(), null);
					log.debug("Recommendation = "+recommendation);					
					monoList.add(integration.createRecommendation(recommendation));
				});
			}

			if (body.getReviews() != null) {
				body.getReviews().forEach(r -> {
					Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(),
							r.getContent(), null);
					log.debug("Review = "+review);					
					monoList.add(integration.createReview(review));
				});
			}

			//monoList.forEach(mono -> mono.block());
			log.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());
			
	      return Mono.zip(
	    		  	r -> "", 
	    		  	monoList.toArray(new Mono[0]))
					.log(log.getName(), FINE)
	    	        .doOnError(ex -> log.warn("createCompositeProduct failed: {}", ex.toString()))
	    	        .then();
			
		} catch (RuntimeException re) {
			log.warn("createCompositeProduct failed", re);
			throw re;
		}
	}

	@Override
	public Mono<ProductAggregate> getProduct(int productId) {
		log.debug("Getting Aggregate Product with productId = {} on {}", productId, serviceUtil.getServiceAddress());

	    return Mono.zip(
	    	      values -> createProductAggregate((Product) values[0], (List<Recommendation>) values[1], (List<Review>) values[2], serviceUtil.getServiceAddress()),
	    	      integration.getProduct(productId), // values[0]
	    	      integration.getRecommendations(productId).collectList(), // values[1]
	    	      integration.getReviews(productId).collectList()) // values[2]
	    	      .doOnError(ex -> log.warn("getCompositeProduct failed: {}", ex.toString()))
	    	      .log(log.getName(), FINE);		
	}

	@Override
	public Mono<Void> deleteProduct(int productId) {

	   try {

		  log.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

		  return Mono.zip(
		        r -> "",
		        integration.deleteProduct(productId),
		        integration.deleteRecommendations(productId),
		        integration.deleteReviews(productId))
		        .doOnError(ex -> log.warn("delete failed: {}", ex.toString()))
		        .log(log.getName(), FINE).then();

	    } catch (RuntimeException re) {
	      log.warn("deleteCompositeProduct failed: {}", re.toString());
	      throw re;
	    }
	}

	private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations,List<Review> reviews, String serviceAddress) {

		log.debug("Creating Product : "+product.toString());
		// 1. Setup product info
		int productId = product.getProductId();
		String name = product.getName();
		int weight = product.getWeight();

		// 2. Copy summary recommendation info, if available
		List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null
				// iterate over list
				: recommendations.stream()
						.map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(),r.getContent()))
						.collect(Collectors.toList());

		// 3. Copy summary review info, if available
		List<ReviewSummary> reviewSummaries = (reviews == null) ? null
				// iterate over list
				: reviews.stream().map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(),r.getContent()))
						.collect(Collectors.toList());

		// 4. Create info regarding the involved microservices addresses
		String productAddress = product.getServiceAddress();
		String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
		String recommendationAddress = (recommendations != null && recommendations.size() > 0)
				? recommendations.get(0).getServiceAddress()
				: "";
		ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress,
				recommendationAddress);

		return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries,
				serviceAddresses);
	}
}
