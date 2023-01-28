package be.snife.sbms.recommendation.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "recommendations")
@CompoundIndex(name = "prod-rec-id", unique = true, def = "{'productId': 1, 'recommendationId' : 1}")
@NoArgsConstructor
public class RecommendationEntity {

  @Id
  @Getter @Setter
  private String id;

  @Version
  @Getter @Setter
  private Integer version;

  @Getter @Setter
  private int productId;
  @Getter @Setter
  private int recommendationId;
  @Getter @Setter
  private String author;
  @Getter @Setter
  private int rating;
  @Getter @Setter
  private String content;

  public RecommendationEntity(int productId, int recommendationId, String author, int rating, String content) {
    this.productId = productId;
    this.recommendationId = recommendationId;
    this.author = author;
    this.rating = rating;
    this.content = content;
  }
  
  @Override
  public String toString() {
    return String.format("RecommendationEntity: %s/%d", productId, recommendationId);
  }  


}
