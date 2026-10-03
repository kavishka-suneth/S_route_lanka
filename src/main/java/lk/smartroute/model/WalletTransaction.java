package lk.smartroute.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="wallet_transactions", uniqueConstraints=@UniqueConstraint(name="uk_wallet_tx_reference",columnNames="transaction_reference"), indexes=@Index(name="idx_wallet_tx_wallet_date",columnList="wallet_id,created_at"))
@AttributeOverride(name="id", column=@Column(name="wallet_transaction_id",nullable=false,updatable=false))
@Getter @Setter @NoArgsConstructor
public class WalletTransaction extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="wallet_id",nullable=false) private Wallet wallet;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="booking_id") private Booking booking;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="card_id") private CustomerCard card;
 @Column(name="transaction_reference",nullable=false,length=100) private String transactionReference;
 @Enumerated(EnumType.STRING) @Column(name="transaction_type",nullable=false,length=30) private WalletTransactionType transactionType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private WalletTransactionStatus status;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 @Column(name="balance_after",nullable=false,precision=19,scale=2) private BigDecimal balanceAfter;
 @Column(name="related_reference",length=100) private String relatedReference;
}
