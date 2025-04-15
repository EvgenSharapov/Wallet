package org.example.wallet.repository;

import org.example.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.id = :id")
    int updateBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount " +
            "WHERE w.id = :id AND w.balance >= :amount")
    int withdrawWithCheck(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Query("SELECT w.balance FROM Wallet w WHERE w.id = :id")
    Optional<BigDecimal> findBalanceById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);
}