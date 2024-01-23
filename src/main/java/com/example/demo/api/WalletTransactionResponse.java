package com.example.demo.api;

import com.example.demo.model.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Models API response entity for a wallet transaction.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletTransactionResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
