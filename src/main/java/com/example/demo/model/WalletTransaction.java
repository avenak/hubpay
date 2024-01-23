package com.example.demo.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Models a single transaction in a customer's virtual wallet.
@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // Always use BigDecimal for financial values (not Double or Float)
    private BigDecimal amount;

    private LocalDateTime timestamp;

    protected WalletTransaction() {}

    /**
     * Constructor. Services must use this constructor to prepare a wallet transaction. The expectation is that the
     * transaction will be persisted more or less immediately, hence this constructor automatically sets timestamp to
     * current local datetime.
     *
     * @param wallet the wallet for which the transaction applies.
     * @param amount the transaction amount.
     */
    public WalletTransaction(Wallet wallet, BigDecimal amount) {
        this.wallet = wallet;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    // Explicit getters/setters - using Lombok with JPA/Hibernate entity classes is not a good idea.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
