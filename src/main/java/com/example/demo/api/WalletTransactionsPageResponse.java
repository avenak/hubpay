package com.example.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Models API response entity for a page of wallet transactions.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletTransactionsPageResponse {
    int pageNumber;
    int pageSize;
    List<WalletTransactionResponse> transactions;
}
