package com.example.demo.model;

import javax.persistence.*;
import java.math.BigDecimal;

// Models a customer virtual wallet.
// - assume one wallet per customer for the purposes of this demo.
@Entity
@Table(name = "wallet")
public class Wallet {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Always use BigDecimal for financial values (not Double or Float)
    private BigDecimal balance;

    // Explicit getters/setters - using Lombok with JPA/Hibernate entity classes is not a good idea.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
