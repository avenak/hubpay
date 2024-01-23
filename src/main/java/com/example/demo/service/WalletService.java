package com.example.demo.service;

import com.example.demo.exception.ValidationException;
import com.example.demo.model.Wallet;
import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.WalletRepository;
import com.example.demo.repository.WalletTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WalletService {
    // In Production app, would likely get these min/max amounts from external config source.
    public static final BigDecimal MINIMUM_DEPOSIT_AMOUNT = BigDecimal.valueOf(10);
    public static final BigDecimal MAXIMUM_DEPOSIT_AMOUNT = BigDecimal.valueOf(10000);

    // Not explicitly stated in requirements but a withdrawal of zero makes no sense. Use of value to 2dp is arbitrary
    // and reasonable for most currencies.
    public static final BigDecimal MINIMUM_WITHDRAWAL_AMOUNT = new BigDecimal("0.01");
    public static final BigDecimal MAXIMUM_WITHDRAWAL_AMOUNT = BigDecimal.valueOf(5000);
    // Arbitrarily set to 3 seconds to facilitate testing but might be longer in reality (or maybe not)
    public static final int DOUBLE_SUBMIT_GUARD_PERIOD_SECONDS = 3;
    public static final int DEFAULT_PAGE_SIZE = 10;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    // Implemented to support test that embedded database initialised correctly.
    public List<Wallet> listWallets() {
        return walletRepository.findAll();
    }

    /**
     * Gets wallet with specified id.
     *
     * @param walletId id of wallet to add funds to (for demo - same as customer id).
     * @return wallet or {@code null} if no wallet found with specified id.
     */
    public Wallet getWallet(Long walletId) {
        return walletRepository.findById(walletId).orElse(null);
    }

    /**
     * Adds funds to customer wallet. Validates that amount being deposited is not less than a minimum amount and not
     * more than a maximum amount.
     *
     * @param walletId id of wallet to add funds to (for demo - same as customer id).
     * @param amount amount of funds to add.
     * @return the customer's wallet after the transaction has been committed, or {@code null} if no wallet exists
     * with specified id.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Wallet addFunds(Long walletId, BigDecimal amount) {
        // Amount validation
        // As the amount constraints are operation-dependant, validating in service layer is not unreasonable but in
        // Production, it would probably be better to use Javax Validation annotations (possibly custom validators)
        // at entity level.
        if (amount.compareTo(MINIMUM_DEPOSIT_AMOUNT) < 0) {
            throw new ValidationException("Deposit amount must be at least " + MINIMUM_DEPOSIT_AMOUNT);
        } else if (amount.compareTo(MAXIMUM_DEPOSIT_AMOUNT) > 0) {
            throw new ValidationException("Deposit amount must not exceed " + MAXIMUM_DEPOSIT_AMOUNT);
        }

        return processTransaction(walletId, amount);
    }

    /**
     * Withdraws funds from customer wallet. Validates that amount being withdrawn is greater than zero and less than
     * a maximum amount.
     *
     * @param walletId id of wallet to withdraw funds from (for demo - same as customer id).
     * @param amount amount of funds to withdraw (defined as a positive number).
     * @return the customer's wallet after the transaction has been committed, or {@code null} if no wallet exists
     * with specified id.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Wallet withdrawFunds(Long walletId, BigDecimal amount) {
        // Amount validation
        // As the amount constraints are operation-dependant, validating in service layer is not unreasonable but in
        // Production, it would probably be better to use Javax Validation annotations (possibly custom validators)
        // at entity level.
        if (amount.compareTo(MINIMUM_WITHDRAWAL_AMOUNT) < 0) {
            throw new ValidationException("Withdrawal amount must be at least " + MINIMUM_WITHDRAWAL_AMOUNT);
        } else if (amount.compareTo(MAXIMUM_WITHDRAWAL_AMOUNT) > 0) {
            throw new ValidationException("Withdrawal amount must not exceed " + MAXIMUM_WITHDRAWAL_AMOUNT);
        }

        return processTransaction(walletId, amount.negate());
    }

    /**
     * Retrieves page of wallet transactions in descending order of timestamp.
     *
     * @param walletId id of wallet to list transactions for (for demo - same as customer id).
     * @param pageNumber page number (first page is page number zero)
     * @param pageSize number of results per page
     * @return list of {@code WalletTransaction} or {@code null} if no wallet exists with specified id.
     */
    public List<WalletTransaction> pageWalletTransactions(Long walletId, int pageNumber, int pageSize) {
        // Fail fast if no wallet exists for specified id.
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);

        if (!walletOpt.isPresent()) {
            return null;
        }

        Wallet wallet = walletOpt.get();

        return walletTransactionRepository.findAllByWalletOrderByTimestampDesc(wallet, PageRequest.of(pageNumber, pageSize));
    }

    // Performs common fund transaction processing - including double-submit guard
    private Wallet processTransaction(Long walletId, BigDecimal amount) {
        // Fail fast if no wallet exists for specified id.
        Optional<Wallet> walletOpt = walletRepository.findById(walletId);

        if (!walletOpt.isPresent()) {
            return null;
        }

        // Double-Submit guard check
        Wallet wallet = walletOpt.get();

        LocalDateTime currentTimestamp = LocalDateTime.now();

        // Get the latest transaction for wallet (there might not be one)
        List<WalletTransaction> transactions =
                walletTransactionRepository.findAllByWalletOrderByTimestampDesc(wallet, PageRequest.of(0, 1));

        if (!transactions.isEmpty()) {
            WalletTransaction latestTransaction = transactions.get(0);

            // Perform double-submit check - if latest transaction was for the same amount and occurred within the past
            // N seconds, throw validation exception (on the basis that it is probably a double-submit).
            // (This may not be the right business logic, but it should suffice for demo purposes.)
            if ((latestTransaction.getAmount().compareTo(amount) == 0) &&
                    (Duration.between(latestTransaction.getTimestamp(), currentTimestamp).toMillis()
                            < (DOUBLE_SUBMIT_GUARD_PERIOD_SECONDS * 1000))) {
                throw new ValidationException("Transaction rejected - possible double-submit");
            }
        }

        // Determine new balance and ensure it is not less than zero.
        BigDecimal newBalance = wallet.getBalance().add(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Withdrawal amount exceeds available balance");
        }

        wallet.setBalance(wallet.getBalance().add(amount));

        walletTransactionRepository.save(new WalletTransaction(wallet, amount));

        return walletRepository.save(wallet);
    }
}
