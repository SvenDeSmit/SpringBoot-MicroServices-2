package be.snife.sbms.api.composite.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "ProductComposite", description = "REST API for composite product information")
public interface ProductCompositeService {

	/**
	 * Sample usage, see below.
	 *
	 * curl -X POST $HOST:$PORT/product-composite \ -H "Content-Type:
	 * application/json" --data \ '{"productId":123,"name":"product
	 * 123","weight":123}'
	 *
	 * @param body A JSON representation of the new composite product
	 */
	@Operation(summary = "Creates a composite product.", description = """
			 # Normal response
			  The composite product information posted to the API will be split up and stored as separate product-info, recommendation and review entities.

			  # Expected error responses
			  1. If a product with the same productId as specified in the posted information already exists,
			     an **422 - Unprocessable Entity** error with a "duplicate key" error message will be returned

			""")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
			@ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}") })
	@PostMapping(value = "/product-composite", consumes = "application/json")
	@ResponseStatus(HttpStatus.ACCEPTED)
	Mono<Void> createProduct(@RequestBody ProductAggregate body);

	/**
	 * Sample usage: "curl $HOST:$PORT/product-composite/1".
	 *
	 * @param productId Id of the product
	 * @return the composite product info, if found, else null
	 */
	@Operation(summary = "Returns a composite view of the specified product id", description = """
			   # Normal response
			   If the requested product id is found the method will return information regarding:
			   1. Base product information
			   1. Reviews
			   1. Recommendations
			   1. Service Addresses\n(technical information regarding the addresses of the microservices that created the response)

			   # Expected partial and error responses
			   In the following cases, only a partial response be created (used to simplify testing of error conditions)

			   ## Product id 113
			   200 - Ok, but no recommendations will be returned

			   ## Product id 213
			   200 - Ok, but no reviews will be returned

			   ## Non numerical product id
			   400 - A **Bad Request** error will be returned

			   ## Product id 13
			   404 - A **Not Found** error will be returned

			   ## Negative product ids
			   422 - An **Unprocessable Entity** error will be returned
			""")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "400", description = "Bad Request, invalid format of the request. See response message for more information"),
			@ApiResponse(responseCode = "404", description = "Not found, the specified id does not exist"),
			@ApiResponse(responseCode = "422", description = "Unprocessable entity, input parameters caused the processing to fail. See response message for more information") })
	@GetMapping(value = "/product-composite/{productId}", produces = "application/json")
	Mono<ProductAggregate> getProduct(@PathVariable int productId);

	/**
	 * Sample usage: "curl -X DELETE $HOST:$PORT/product-composite/1".
	 *
	 * @param productId Id of the product
	 */
	@Operation(summary = "Deletes a product composite", description = """
			  # Normal response
			  Entities for product information, recommendations and reviews related to the specified productId will be deleted.
			  The implementation of the delete method is idempotent, i.e. it can be called several times with the same response.

			  This means that a delete request of a non-existing product will return **200 Ok**.
			""")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
			@ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}") })
	@DeleteMapping(value = "/product-composite/{productId}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	Mono<Void> deleteProduct(@PathVariable int productId);

}
