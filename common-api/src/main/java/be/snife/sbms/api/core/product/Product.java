package be.snife.sbms.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class Product {
	@Getter	@Setter
	private int productId;
	@Getter	@Setter
	private String name;
	@Getter @Setter
	private int weight;
	@Getter @Setter
	private String serviceAddress;

	public Product() {
		productId = 0;
		name = null;
		weight = 0;
		serviceAddress = null;
	}

}
