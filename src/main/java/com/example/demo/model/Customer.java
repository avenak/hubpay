package com.example.demo.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

// Models a customer.
@Entity
@Table(name = "customer")
public class Customer {
    @Id
    private Long id;

    private String name;

    // Explicit getters/setters - using Lombok with JPA/Hibernate entity classes is not a good idea.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
