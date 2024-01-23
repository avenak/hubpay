package com.example.demo.controller;

import com.example.demo.api.*;
import com.example.demo.exception.ValidationException;
import com.example.demo.model.Wallet;
import com.example.demo.model.WalletTransaction;
import com.example.demo.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> balance(@PathVariable("id") Long id) {
        try {
            Wallet wallet = walletService.getWallet(id);

            // As per WalletService contract if 'getWallet' method returns null, it is because no wallet with specified
            // id was found - emit NOT_FOUND error response.
            if (wallet == null) {
                return new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Wallet does not exist"), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new WalletResponse(wallet.getBalance()), HttpStatus.OK);
        } catch (Exception ex) {
            // Unknown exception - in Production, would probably explicitly log this with full details and/or send
            // notification (SNS topic?) to trigger an alarm (on the basis that a truly unknown exception should
            // rarely, if ever, happen).
            return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> transactions(@PathVariable("id") Long id,
                                          @RequestParam(defaultValue = "0") int pageNumber,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<WalletTransaction> transactions = walletService.pageWalletTransactions(id, pageNumber, pageSize);

            // As per WalletService contract if 'pageWalletTransactions' method returns null, it is because no wallet
            // with specified id was found - emit NOT_FOUND error response.
            if (transactions == null) {
                return new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Wallet does not exist"), HttpStatus.NOT_FOUND);
            }

            // Convert transaction list to list of 'WalletTransactionResponse' entities.
            List<WalletTransactionResponse> walletTransactions = transactions.stream()
                    .map(tx -> new WalletTransactionResponse(tx.getId(), tx.getAmount(), tx.getTimestamp()))
                    .collect(Collectors.toList());

            WalletTransactionsPageResponse pageResponse =
                    new WalletTransactionsPageResponse(pageNumber, pageSize, walletTransactions);

            return new ResponseEntity<>(pageResponse, HttpStatus.OK);
        } catch (Exception ex) {
            // Unknown exception - in Production, would probably explicitly log this with full details and/or send
            // notification (SNS topic?) to trigger an alarm (on the basis that a truly unknown exception should
            // rarely, if ever, happen).
            return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable("id") Long id, @RequestBody WalletFundsRequest request) {
        try {
            Wallet wallet = walletService.addFunds(id, request.getAmount());

            // As per WalletService contract if 'addFunds' method returns null, it is because no wallet with specified
            // id was found - emit NOT_FOUND error response.
            if (wallet == null) {
                return new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Wallet does not exist"), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new WalletResponse(wallet.getBalance()), HttpStatus.OK);
        } catch (Exception ex) {
            // If exception is a validation exception, request iw well-formed but contains invalid data (e.g. amount
            // too low or too high) - this can be corrected by client so emit BAD_REQUEST error response.
            if (ex instanceof ValidationException) {
                return new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()), HttpStatus.BAD_REQUEST);
            }

            // Unknown exception - in Production, would probably explicitly log this with full details and/or send
            // notification (SNS topic?) to trigger an alarm (on the basis that a truly unknown exception should
            // rarely, if ever, happen).
            return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable("id") Long id, @RequestBody WalletFundsRequest request) {
        try {
            Wallet wallet = walletService.withdrawFunds(id, request.getAmount());

            // As per WalletService contract if 'withdrawFunds' method returns null, it is because no wallet with
            // specified id was found - emit NOT_FOUND error response.
            if (wallet == null) {
                return new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Wallet does not exist"), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(new WalletResponse(wallet.getBalance()), HttpStatus.OK);
        } catch (Exception ex) {
            // If exception is a validation exception, request iw well-formed but contains invalid data (e.g. amount
            // too low or too high) - this can be corrected by client so emit BAD_REQUEST error response.
            if (ex instanceof ValidationException) {
                return new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()), HttpStatus.BAD_REQUEST);
            }

            // Unknown exception - in Production, would probably explicitly log this with full details and/or send
            // notification (SNS topic?) to trigger an alarm (on the basis that a truly unknown exception should
            // rarely, if ever, happen).
            return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
