package org.example.wallet.repository;

import org.example.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query(value = "INSERT INTO wallets (id, balance, version, created_at, updated_at) " +
            "VALUES (:id, 0, 0, NOW(), NOW()) " +
            "ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void createWalletIfNotExists(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}