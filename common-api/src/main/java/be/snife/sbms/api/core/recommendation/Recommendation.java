package be.snife.sbms.api.core.recommendation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class Recommendation {

	@Getter @Setter
	private int productId;
	@Getter @Setter
	private int recommendationId;
	@Getter @Setter
	private String author;
	@Getter @Setter
	private int rate;
	@Getter @Setter
	private String content;
	@Getter @Setter
	private String serviceAddress;

	public Recommendation() {
		productId = 0;
		recommendationId = 0;
		author = null;
		rate = 0;
		content = null;
		serviceAddress = null;
	}

}
