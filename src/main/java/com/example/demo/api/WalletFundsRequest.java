package com.example.demo.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Models an API request object for wallet funds operation request (i.e. deposit or withdrawal). This class would be
 * annotated with Swagger annotations for API documentation.
 */
@Data
public class WalletFundsRequest {
    private BigDecimal amount;
}
