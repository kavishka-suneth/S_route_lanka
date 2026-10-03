package lk.smartroute.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="customer_cards", indexes=@Index(name="idx_customer_cards_user",columnList="user_id"), uniqueConstraints=@UniqueConstraint(name="uk_customer_cards_token",columnNames="card_token"))
@AttributeOverride(name="id", column=@Column(name="card_id",nullable=false,updatable=false))
@Getter @Setter @NoArgsConstructor
public class CustomerCard extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="cardholder_name",nullable=false,length=120) private String cardholderName;
 @Enumerated(EnumType.STRING) @Column(name="card_brand",nullable=false,length=20) private CardBrand brand;
 @Column(name="last_four",nullable=false,length=4) private String lastFour;
 @Column(name="expiry_month",nullable=false) private int expiryMonth;
 @Column(name="expiry_year",nullable=false) private int expiryYear;
 @Column(name="card_token",nullable=false,length=100) private String cardToken;
 @Column(nullable=false) private boolean verified=true;
}
