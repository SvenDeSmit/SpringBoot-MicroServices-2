package be.snife.sbms.review.persistence;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
// define composite business key as index
@Table(name = "reviews", indexes = { @Index(name = "reviews_unique_idx", unique = true, columnList = "productId,reviewId") })
@NoArgsConstructor

public class ReviewEntity {

  @Id @GeneratedValue
  @Getter @Setter
  // PK
  public int id;

  @Version
  @Getter @Setter
  // Optimistic locking
  public int version;

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

  public ReviewEntity(int productId, int reviewId, String author, String subject, String content) {
    this.productId = productId;
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;
  }


}
