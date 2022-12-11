package be.snife.sbms.api.composite.product;

import lombok.Getter;
import lombok.Setter;

public class RecommendationSummary {

	@Getter	@Setter
	private int recommendationId;
	@Getter	@Setter
	private String author;
	@Getter	@Setter
	private int rate;
	@Getter	@Setter
	private String content;

	
  public RecommendationSummary() {
	    this.recommendationId = 0;
	    this.author = null;
	    this.rate = 0;
	    this.content = null;
	  }
	
	public RecommendationSummary(int recommendationId, String author, int rate,String content) {
		this.recommendationId = recommendationId;
		this.author = author;
		this.rate = rate;
		this.content = content;

	}

}
