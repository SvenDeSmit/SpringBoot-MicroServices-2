package be.snife.sbms.api.core.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import be.snife.sbms.api.event.Event;
import reactor.core.publisher.Mono;

public interface ProductService {

	/**
	 * Sample usage: "curl $HOST:$PORT/product/1".
	 *
	 * @param productId Id of the product
	 * @return the product, if found, else null
	 */
	@GetMapping(value = "/product/{productId}", produces = "application/json")
	Mono<Product> getProduct(@PathVariable int productId);
	
	/**
	 * Sample usage, see below.
	 *
	 * curl -X POST $HOST:$PORT/product \ -H "Content-Type: application/json" --data
	 * \ '{"productId":123,"name":"product 123","weight":123}'
	 *
	 * @param body A JSON representation of the new product
	 * @return A JSON representation of the newly created product
	 */
	@PostMapping(value = "/product", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	Mono<Product> createProduct(@RequestBody Product body);


	/**
	 * Sample usage: "curl -X DELETE $HOST:$PORT/product/1".
	 *
	 * @param productId Id of the product
	 */
	@DeleteMapping(value = "/product/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> deleteProduct(@PathVariable int productId);
	
	@PostMapping(value = "/product/event", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	Mono<Void> publishProductEvent(@RequestBody Event<Integer, Product> event); 
	

}
