package com.example.demo.service;

import com.example.demo.exception.ValidationException;
import com.example.demo.model.Wallet;
import com.example.demo.model.WalletTransaction;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class WalletServiceTest {
    @Autowired
    private WalletService walletService;

    @Test
    void list() {
        List<Wallet> wallets = walletService.listWallets();

        Assertions.assertThat(wallets).isNotEmpty();
    }

    @Test
    void addFundsDepositTooLow() {
        Assertions.assertThatThrownBy(
                        () -> walletService.addFunds(1L, WalletService.MINIMUM_DEPOSIT_AMOUNT.subtract(BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addFundsDepositTooHigh() {
        Assertions.assertThatThrownBy(
                        () -> walletService.addFunds(1L, WalletService.MAXIMUM_DEPOSIT_AMOUNT.add(BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addFundsDepositJustRight() {
        // Given a valid deposit amount
        BigDecimal amount = WalletService.MAXIMUM_DEPOSIT_AMOUNT.subtract(BigDecimal.TEN);

        // And current wallet balance
        Wallet before = walletService.getWallet(1L);

        // When funds added
        Wallet after = walletService.addFunds(1L, amount);

        // Then new wallet balance should match expected balance
        BigDecimal expectedBalance = before.getBalance().add(amount);

        Assertions.assertThat(after.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void withdrawFundsWithdrawalTooLow() {
        Assertions.assertThatThrownBy(
                        () -> walletService.withdrawFunds(1L, WalletService.MINIMUM_WITHDRAWAL_AMOUNT.subtract(BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void withdrawFundsWithdrawalTooHigh() {
        Assertions.assertThatThrownBy(
                        () -> walletService.withdrawFunds(1L, WalletService.MAXIMUM_WITHDRAWAL_AMOUNT.add(BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void withdrawFundsBalanceBelowZero() {
        // Given current wallet balance
        Wallet before = walletService.getWallet(1L);

        // And a valid withdrawal amount which exceeds balance
        BigDecimal amount = before.getBalance().add(BigDecimal.TEN);

        Assertions.assertThat(amount).isLessThanOrEqualTo(WalletService.MAXIMUM_WITHDRAWAL_AMOUNT);

        // When attempt made to withdraw funds, then validation exception thrown
        Assertions.assertThatThrownBy(
                        () -> walletService.withdrawFunds(1L, amount))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void withdrawFundsWithdrawalJustRight() {
        // Given current wallet balance
        Wallet before = walletService.getWallet(1L);

        // And a withdrawal amount that does not exceed wallet balance or max withdrawal amount
        BigDecimal amount = before.getBalance().subtract(BigDecimal.TEN)
                .min(WalletService.MAXIMUM_WITHDRAWAL_AMOUNT.subtract(BigDecimal.TEN));

        // When funds withdrawn
        Wallet after = walletService.withdrawFunds(1L, amount);

        // Then new wallet balance should match expected balance
        BigDecimal expectedBalance = before.getBalance().subtract(amount);

        Assertions.assertThat(after.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void withdrawFundsBalanceToZero() {
        // Given current wallet balance
        Wallet before = walletService.getWallet(3L);

        // When entire balance withdrawn
        Wallet after = walletService.withdrawFunds(3L, before.getBalance());

        // Then new wallet balance should be zero
        Assertions.assertThat(after.getBalance()).isEqualTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    void listWalletTransactions() {
        // Given an existing wallet...
        Long walletId = 2L;
        Wallet before = walletService.getWallet(walletId);

        // With zero or more transactions...
        List<WalletTransaction> transactions = walletService.pageWalletTransactions(walletId, 0, 50);

        int originalTransactionCount = transactions.size();

        // And three additional transactions created (so there are at least some transactions)
        // (Need to be different amounts to pass double-guard check.)
        BigDecimal amount = WalletService.MINIMUM_DEPOSIT_AMOUNT;

        walletService.addFunds(walletId, amount);
        walletService.addFunds(walletId, amount.multiply(BigDecimal.valueOf(2)));
        walletService.withdrawFunds(walletId, amount.multiply(BigDecimal.valueOf(3)));

        // When page of wallet transactions now retrieved
        transactions = walletService.pageWalletTransactions(walletId, 0, 50);

        // There should be N more transactions than there were originally...
        Assertions.assertThat(transactions).hasSize(originalTransactionCount + 3);

        // And timestamp of first transaction in list should be later than that of second transaction...
        Assertions.assertThat(transactions.get(0).getTimestamp()).isAfter(transactions.get(1).getTimestamp());

        // NB: This final assertion is just a sanity check - not trying to test JPA but does give confidence that
        //     repository method has been defined correctly (i.e. sorting transactions in timestamp desc order).
    }

    @Test
    void doubleSubmitCheckSameTransactionRepeatedImmediately() {
        Assertions.assertThatThrownBy(
                        () -> {
                            walletService.addFunds(1L, WalletService.MINIMUM_DEPOSIT_AMOUNT);
                            walletService.addFunds(1L, WalletService.MINIMUM_DEPOSIT_AMOUNT);
                        })
                .isInstanceOf(ValidationException.class)
                .hasMessage("Transaction rejected - possible double-submit");
    }

    @Test
    void doubleSubmitCheckSameTransactionRepeatedAfterSufficientDelay() throws Exception {
        // Given a valid deposit amount
        BigDecimal amount = WalletService.MINIMUM_DEPOSIT_AMOUNT;

        // And current wallet balance
        Wallet before = walletService.getWallet(1L);

        // When funds added...
        walletService.addFunds(1L, amount);

        // And, after a sufficient delay (more than double-submit check period), add same amount of funds again
        // (Although sleep could be interrupted at any time, it's unlikely there will be sufficient thread/process
        // contention at the time these tests are running.)
        Thread.sleep((WalletService.DOUBLE_SUBMIT_GUARD_PERIOD_SECONDS + 1) * 1000);

        Wallet after = walletService.addFunds(1L, amount);

        // Then new wallet balance should match expected balance
        BigDecimal expectedBalance = before.getBalance().add(amount.multiply(BigDecimal.valueOf(2)));

        Assertions.assertThat(after.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void doubleSubmitCheckDifferentTransactionMadeImmediately() throws Exception {
        // Given a valid deposit amount
        BigDecimal amount = WalletService.MINIMUM_DEPOSIT_AMOUNT;

        // And current wallet balance
        Wallet before = walletService.getWallet(1L);

        // When funds added...
        walletService.addFunds(1L, amount);

        // And then same funds withdrawn immediately
        Wallet after = walletService.withdrawFunds(1L, amount);

        // Then no double-submit exception thrown,
        // And new wallet balance should match original balance
        BigDecimal expectedBalance = before.getBalance();

        Assertions.assertThat(after.getBalance()).isEqualTo(expectedBalance);
    }
}
