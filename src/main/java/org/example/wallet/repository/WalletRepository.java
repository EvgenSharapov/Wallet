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
    @Query(nativeQuery = true, value = """
        WITH inserted AS (
            INSERT INTO wallets (id, balance, version, created_at, updated_at)
            VALUES (:id, 0, 1, NOW(), NOW())
            ON CONFLICT (id) DO NOTHING
            RETURNING id
        )
        UPDATE wallets
        SET balance = balance + :amount,
            version = version + 1,
            updated_at = NOW()
        WHERE id = :id
        RETURNING balance""")
    Optional<BigDecimal> depositAmount(@Param("id") UUID id,
                                       @Param("amount") BigDecimal amount);

    @Modifying
    @Query(nativeQuery = true, value = """
    UPDATE wallets 
    SET balance = balance - :amount,
        version = version + 1
    WHERE id = :id AND balance >= :amount""")
    int withdrawAmount(@Param("id") UUID id,
                       @Param("amount") BigDecimal amount);

    @Query("SELECT balance FROM Wallet WHERE id = :id")
    Optional<BigDecimal> findBalanceById(@Param("id") UUID id);
}