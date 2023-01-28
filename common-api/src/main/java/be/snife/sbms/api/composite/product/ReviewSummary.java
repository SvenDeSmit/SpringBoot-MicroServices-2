package be.snife.sbms.api.composite.product;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ReviewSummary {

	@Getter
	@Setter
	private int reviewId;
	@Getter
	@Setter
	private String author;
	@Getter
	@Setter
	private String subject;
	@Getter
	@Setter
	private String content;

	public ReviewSummary() {
		this.reviewId = 0;
		this.author = null;
		this.subject = null;
		this.content = null;
	}

	public ReviewSummary(int reviewId, String author, String subject, String content) {
		this.reviewId = reviewId;
		this.author = author;
		this.subject = subject;
		this.content = content;
	}

}
