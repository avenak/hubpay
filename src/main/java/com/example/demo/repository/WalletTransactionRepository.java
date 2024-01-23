package com.example.demo.repository;

import com.example.demo.model.Wallet;
import com.example.demo.model.WalletTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    // For demo purposes, electing to use List response instead of Page or Slice - assuming transactions for a single
    // wallet are unlikely to be in the hundreds of thousands or millions, a Page response (with the overhead of an
    // extra count query) would probably be ok.
    List<WalletTransaction> findAllByWalletOrderByTimestampDesc(Wallet wallet, Pageable pageable);
}
