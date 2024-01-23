package com.example.demo.controller;

import com.example.demo.api.ErrorResponse;
import com.example.demo.api.WalletResponse;
import com.example.demo.api.WalletTransactionsPageResponse;
import com.example.demo.service.WalletService;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.net.URL;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static HttpHeaders headers;

    @BeforeAll
    static void beforeAll() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void balanceWalletNotFound() throws Exception {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                getBalanceUrl(), ErrorResponse.class, -1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        Assertions.assertThat(response.getBody().getMessage()).isEqualTo("Wallet does not exist");
    }

    @Test
    public void balanceWalletExists() throws Exception {
        ResponseEntity<WalletResponse> response = restTemplate.getForEntity(
                getBalanceUrl(), WalletResponse.class, 1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void depositWalletNotExists() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(
                getWalletFundsRequestJSON(WalletService.MINIMUM_DEPOSIT_AMOUNT), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getDepositUrl(), request, ErrorResponse.class, -1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        Assertions.assertThat(response.getBody().getMessage()).isEqualTo("Wallet does not exist");
    }

    @Test
    public void depositAmountBelowMinimumRequired() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(
                getWalletFundsRequestJSON(WalletService.MINIMUM_DEPOSIT_AMOUNT.subtract(BigDecimal.TEN)), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getDepositUrl(), request, ErrorResponse.class, 1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Assertions.assertThat(response.getBody().getMessage()).startsWith("Deposit amount must be at least");
    }

    @Test
    public void depositAmountExceedsMaxAllowed() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(
                getWalletFundsRequestJSON(WalletService.MAXIMUM_DEPOSIT_AMOUNT.add(BigDecimal.TEN)), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getDepositUrl(), request, ErrorResponse.class, 1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Assertions.assertThat(response.getBody().getMessage()).startsWith("Deposit amount must not exceed");
    }

    @Test
    public void depositAmountValid() throws Exception {
        BigDecimal depositAmount = WalletService.MAXIMUM_DEPOSIT_AMOUNT.subtract(BigDecimal.TEN);

        HttpEntity<String> request = new HttpEntity<>(getWalletFundsRequestJSON(depositAmount), headers);

        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                getDepositUrl(), request, WalletResponse.class, 1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getBalance()).isGreaterThanOrEqualTo(depositAmount);
    }

    @Test
    public void withdrawWalletNotExists() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(
                getWalletFundsRequestJSON(WalletService.MINIMUM_WITHDRAWAL_AMOUNT), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getWithdrawUrl(), request, ErrorResponse.class, -1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        Assertions.assertThat(response.getBody().getMessage()).isEqualTo("Wallet does not exist");
    }

    @Test
    public void withdrawAmountBelowMinimumRequired() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(
                getWalletFundsRequestJSON(WalletService.MINIMUM_WITHDRAWAL_AMOUNT.subtract(BigDecimal.TEN)), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getWithdrawUrl(), request, ErrorResponse.class, 1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Assertions.assertThat(response.getBody().getMessage()).startsWith("Withdrawal amount must be at least");
    }

    @Test
    public void withdrawAmountExceedsMaxAllowed() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(
                getWalletFundsRequestJSON(WalletService.MAXIMUM_WITHDRAWAL_AMOUNT.add(BigDecimal.TEN)), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getWithdrawUrl(), request, ErrorResponse.class, 1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Assertions.assertThat(response.getBody().getMessage()).startsWith("Withdrawal amount must not exceed");
    }

    @Test
    public void withdrawAmountValid() throws Exception {
        Long walletId = 3L;
        BigDecimal currentBalance = getWalletBalance(walletId);

        // Withdraw half the balance (but no more than maximum allowed)
        BigDecimal withdrawAmount = currentBalance.divide(BigDecimal.valueOf(2), BigDecimal.ROUND_DOWN)
                .min(WalletService.MAXIMUM_WITHDRAWAL_AMOUNT);

        System.out.println("Withdraw amount: " + withdrawAmount);

        HttpEntity<String> request = new HttpEntity<>(getWalletFundsRequestJSON(withdrawAmount), headers);

        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                getWithdrawUrl(), request, WalletResponse.class, walletId);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getBalance()).isEqualTo(currentBalance.subtract(withdrawAmount));
    }

    @Test
    public void withdrawToZero() throws Exception {
        Long walletId = 2L;
        BigDecimal currentBalance = getWalletBalance(walletId);

        System.out.println("Current balance: " + currentBalance);

        // Withdraw the full balance, no more, no less
        HttpEntity<String> request = new HttpEntity<>(getWalletFundsRequestJSON(currentBalance), headers);

        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                getWithdrawUrl(), request, WalletResponse.class, walletId);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getBalance()).isEqualTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    public void withdrawAmountExceedsAvailableBalance() throws Exception {
        Long walletId = 1L;
        BigDecimal currentBalance = getWalletBalance(walletId);

        // Withdraw more than the available balance
        BigDecimal withdrawAmount = currentBalance.add(BigDecimal.TEN);

        HttpEntity<String> request = new HttpEntity<>(getWalletFundsRequestJSON(withdrawAmount), headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getWithdrawUrl(), request, ErrorResponse.class, walletId);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Assertions.assertThat(response.getBody().getMessage()).isEqualTo("Withdrawal amount exceeds available balance");
    }

    @Test
    public void transactionsWalletNotFound() throws Exception {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                getTransactionsUrl(), ErrorResponse.class, -1);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        Assertions.assertThat(response.getBody().getMessage()).isEqualTo("Wallet does not exist");
    }

    @Test
    public void transactionsDefaultPagination() throws Exception {
        // Add funds to existing wallet (so the presence of at least one transaction in response can be verified)
        Long walletId = 1L;

        addFunds(walletId, WalletService.MINIMUM_DEPOSIT_AMOUNT);

        // Now get transactions
        ResponseEntity<WalletTransactionsPageResponse> response = restTemplate.getForEntity(
                getTransactionsUrl(), WalletTransactionsPageResponse.class, walletId);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getPageNumber()).isEqualTo(0);
        Assertions.assertThat(response.getBody().getPageSize()).isEqualTo(WalletService.DEFAULT_PAGE_SIZE);
        Assertions.assertThat(response.getBody().getTransactions()).hasSizeGreaterThan(0);
    }

    @Test
    public void transactionsSpecificPagination() throws Exception {
        // Add funds multiple times to existing wallet
        Long walletId = 2L;
        int pageSize = 50;

        // (Different amounts to pass double-submit check.)
        addFunds(walletId, WalletService.MINIMUM_DEPOSIT_AMOUNT.add(BigDecimal.ONE));
        addFunds(walletId, WalletService.MINIMUM_DEPOSIT_AMOUNT.add(BigDecimal.valueOf(2)));
        addFunds(walletId, WalletService.MINIMUM_DEPOSIT_AMOUNT.add(BigDecimal.valueOf(3)));
        addFunds(walletId, WalletService.MINIMUM_DEPOSIT_AMOUNT.add(BigDecimal.valueOf(4)));

        // Now get all transactions in one page
        ResponseEntity<WalletTransactionsPageResponse> response = restTemplate.getForEntity(
                getTransactionsUrlWithPaginationParams(), WalletTransactionsPageResponse.class,
                walletId, 0, 50);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getPageNumber()).isEqualTo(0);
        Assertions.assertThat(response.getBody().getPageSize()).isEqualTo(pageSize);
        Assertions.assertThat(response.getBody().getTransactions()).hasSizeGreaterThanOrEqualTo(4);

        // Get id of 3rd transaction in list
        Long transaction3Id = response.getBody().getTransactions().get(2).getId();

        // Now get second page of transactions with page size 2 (3rd transaction should be first one on that page)
        response = restTemplate.getForEntity(
                getTransactionsUrlWithPaginationParams(), WalletTransactionsPageResponse.class,
                walletId, 1, 2);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getPageNumber()).isEqualTo(1);
        Assertions.assertThat(response.getBody().getPageSize()).isEqualTo(2);
        Assertions.assertThat(response.getBody().getTransactions()).hasSize(2);
        Assertions.assertThat(response.getBody().getTransactions().get(0).getId()).isEqualTo(transaction3Id);
    }

    private String getBalanceUrl() throws Exception {
        return new URL("http://localhost:" + port + "/api/wallet/{id}").toString();
    }

    private String getTransactionsUrl() throws Exception {
        return new URL("http://localhost:" + port + "/api/wallet/{id}/transactions").toString();
    }

    private String getTransactionsUrlWithPaginationParams() throws Exception {
        return new URL("http://localhost:" + port + "/api/wallet/{id}/transactions?pageNumber={pageNumber}&pageSize={pageSize}").toString();
    }

    private String getDepositUrl() throws Exception {
        return new URL("http://localhost:" + port + "/api/wallet/{id}/deposit").toString();
    }

    private String getWithdrawUrl() throws Exception {
        return new URL("http://localhost:" + port + "/api/wallet/{id}/withdraw").toString();
    }

    private String getWalletFundsRequestJSON(BigDecimal amount) throws Exception {
        JSONObject request = new JSONObject();

        request.put("amount", amount.toString());

        return request.toString();
    }

    // Convenience method to get wallet balance for use in test case
    private BigDecimal getWalletBalance(Long id) throws Exception {
        ResponseEntity<WalletResponse> response = restTemplate.getForEntity(
                getBalanceUrl(), WalletResponse.class, id);

        return response.getBody().getBalance();
    }

    // Convenience method to add funds to a wallet (to facilitate a particular test scenario)
    private void addFunds(Long id, BigDecimal amount) throws Exception {
        HttpEntity<String> request = new HttpEntity<>(getWalletFundsRequestJSON(amount), headers);

        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                getDepositUrl(), request, WalletResponse.class, id);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
