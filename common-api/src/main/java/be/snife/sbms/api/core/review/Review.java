package be.snife.sbms.api.core.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class Review {

	@Getter @Setter
	private int productId;
	@Getter @Setter
	private int reviewId;
	@Getter @Setter
	private String author;
	@Getter @Setter
	private String subject;
	@Getter @Setter
	private String content;
	@Getter @Setter
	private String serviceAddress;

	public Review() {
		productId = 0;
		reviewId = 0;
		author = null;
		subject = null;
		content = null;
		serviceAddress = null;
	}

}
