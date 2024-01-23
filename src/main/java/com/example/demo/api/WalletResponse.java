package com.example.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Models API response entity for a wallet - just balance and no id (internal detail). This class would be annotated
 * with Swagger annotations for API documentation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponse {
    private BigDecimal balance;
}
