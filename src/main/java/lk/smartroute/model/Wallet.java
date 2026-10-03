package lk.smartroute.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="wallets", uniqueConstraints=@UniqueConstraint(name="uk_wallets_user",columnNames="user_id"))
@AttributeOverride(name="id", column=@Column(name="wallet_id",nullable=false,updatable=false))
@Getter @Setter @NoArgsConstructor
public class Wallet extends BaseEntity {
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal balance=BigDecimal.ZERO;
 @Version @Column(nullable=false) private long version;
}
