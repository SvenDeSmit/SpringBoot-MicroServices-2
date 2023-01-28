package be.snife.sbms.productcomposite.services;

import static java.util.logging.Level.FINE;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import be.snife.sbms.api.composite.product.ProductAggregate;
import be.snife.sbms.api.composite.product.ProductCompositeService;
import be.snife.sbms.api.core.product.Product;
import be.snife.sbms.api.core.product.ProductService;
import be.snife.sbms.api.core.recommendation.Recommendation;
import be.snife.sbms.api.core.recommendation.RecommendationService;
import be.snife.sbms.api.core.review.Review;
import be.snife.sbms.api.core.review.ReviewService;
import be.snife.sbms.api.event.Event;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.api.exceptions.NotFoundException;
import be.snife.sbms.util.http.HttpErrorInfo;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {


	private final WebClient webClient;
	private final ObjectMapper mapper;

	private final String productServiceUrl;
	private final String recommendationServiceUrl;
	private final String reviewServiceUrl;

	@Autowired
	public ProductCompositeIntegration(WebClient.Builder webClient, 
			//RestTemplate restTemplate, 
			ObjectMapper mapper,
			@Value("${app.product-service.host}") String productServiceHost,
			@Value("${app.product-service.port}") int productServicePort,
			@Value("${app.recommendation-service.host}") String recommendationServiceHost,
			@Value("${app.recommendation-service.port}") int recommendationServicePort,
			@Value("${app.review-service.host}") String reviewServiceHost,
			@Value("${app.review-service.port}") int reviewServicePort) {

		this.webClient = webClient.build();
		//this.restTemplate = restTemplate;
		this.mapper = mapper;

		productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product";
		recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort
				+ "/recommendation";
		reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review";
	}

	@Override
	public Mono<Product> createProduct(Product body) {

		String url = productServiceUrl;
		log.debug("Will post a new product to URL: {}", url);
		log.debug(""+body.toString());
		return webClient.post().uri(url).body(Mono.just(body), Product.class).retrieve()
				.bodyToMono(Product.class).log(log.getName(), FINE)
				.log(log.getName(), FINE)
				.onErrorMap(WebClientResponseException.class,ex -> handleException(ex));
	}

	@Override
	public Mono<Product> getProduct(int productId) {
		
		String url = productServiceUrl + "/" + productId;
		log.debug("Will call the getProduct API on URL: {}", url);

		return webClient.get().uri(url).retrieve()
					.bodyToMono(Product.class)
					.log(log.getName(), FINE)
					.onErrorMap(WebClientResponseException.class,ex -> handleException(ex));
	}

	@Override
	public Mono<Void> deleteProduct(int productId) {

		String url = productServiceUrl + "/" + productId;
		log.debug("Will call the deleteProduct API on URL: {}", url);

		return webClient.delete().uri(url).retrieve()
				.bodyToMono(Void.class)
				.log(log.getName(), FINE)
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
	}

	
	@Override
	public Mono<Recommendation> createRecommendation(Recommendation body) {

		String url = recommendationServiceUrl;
		log.debug("Will post a new recommendation to URL: {}", url);

		return webClient.post().uri(url).body(Mono.just(body), Recommendation.class).retrieve()
				.bodyToMono(Recommendation.class)
				.log(log.getName(), FINE) 
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
	}

	@Override
	public Flux<Recommendation> getRecommendations(int productId) {

		String url = recommendationServiceUrl + "?productId=" + productId;
		log.debug("Will call the getRecommendations API on URL: {}", url);

		return webClient.get().uri(url).retrieve()
				.bodyToFlux(Recommendation.class)
				.log(log.getName(), FINE)
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
	}

	@Override
	public Mono<Void> deleteRecommendations(int productId) {
		String url = recommendationServiceUrl + "?productId=" + productId;
		log.debug("Will call the deleteRecommendations API on URL: {}", url);

		return webClient.delete().uri(url).retrieve()
				.bodyToMono(Void.class)
				.log(log.getName(), FINE)
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
	}

	@Override
	public Mono<Review> createReview(Review body) {

		String url = reviewServiceUrl;
		log.debug("Will post a new review to URL: {}", url);

		return webClient.post().uri(url).body(Mono.just(body), Review.class).retrieve()
				.bodyToMono(Review.class)
				.log(log.getName(), FINE)
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
	}

	@Override
	public Flux<Review> getReviews(int productId) {

		String url = reviewServiceUrl + "?productId=" + productId;
		log.debug("Will call the getReviews API on URL: {}", url);
		return webClient.get().uri(url).retrieve()
				.bodyToFlux(Review.class)
				.log(log.getName(), FINE)
				.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

	}

	@Override
	public Mono<Void> deleteReviews(int productId) {
			String url = reviewServiceUrl + "?productId=" + productId;
			log.debug("Will call the deleteReviews API on URL: {}", url);
			
			return webClient.delete().uri(url).retrieve()
					.bodyToMono(Void.class)
					.log(log.getName(), FINE)
					.onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

	}

	  private Throwable handleException(Throwable ex) {

		    if (!(ex instanceof WebClientResponseException)) {
		      log.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
		      return ex;
		    }

		    WebClientResponseException wcre = (WebClientResponseException)ex;

		    switch (wcre.getStatusCode()) {

		      case NOT_FOUND:
		        return new NotFoundException(getErrorMessage(wcre));

		      case UNPROCESSABLE_ENTITY :
		        return new InvalidInputException(getErrorMessage(wcre));

		      default:
		        log.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
		        log.warn("Error body: {}", wcre.getResponseBodyAsString());
		        return ex;
		    }
		  }

	  private String getErrorMessage(WebClientResponseException ex) {
		    try {
		      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
		    } catch (IOException ioex) {
		      return ex.getMessage();
		    }
		  }

	  
/*	
	private RuntimeException handleHttpClientException(HttpClientErrorException ex) {
		switch (ex.getStatusCode()) {

		case NOT_FOUND:
			return new NotFoundException(getErrorMessage(ex));

		case UNPROCESSABLE_ENTITY:
			return new InvalidInputException(getErrorMessage(ex));

		default:
			log.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
			log.warn("Error body: {}", ex.getResponseBodyAsString());
			return ex;
		}
	}
*/
	private String getErrorMessage(HttpClientErrorException ex) {
		try {
			return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
		} catch (IOException ioex) {
			return ex.getMessage();
		}
	}

	public Mono<Health> getProductHealth() {
		return getHealth(productServiceUrl);
	}

	public Mono<Health> getRecommendationHealth() {
		return getHealth(recommendationServiceUrl);
	}

	public Mono<Health> getReviewHealth() {
		return getHealth(reviewServiceUrl);
	}

	private Mono<Health> getHealth(String url) {
		url += "/actuator/health";
		log.debug("Will call the Health API on URL: {}", url);
		return webClient.get().uri(url).retrieve()
					.bodyToMono(String.class)
					.map(s -> new Health.Builder().up().build())
				.onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build())).log(log.getName(), FINE);
	}

}
