package org.example.wallet.service;

import org.example.wallet.dto.WalletRequest;
import org.example.wallet.dto.WalletResponse;
import org.example.wallet.exceptions.InsufficientFundsException;
import org.example.wallet.exceptions.WalletNotFoundException;
import org.example.wallet.model.OperationType;
import org.example.wallet.model.Wallet;
import org.example.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final CacheManager cacheManager;

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 5)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100),
            retryFor = {OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public WalletResponse processTransaction(WalletRequest request) {
        UUID walletId = request.getWalletIdAsUUID();

        try {
            if (request.getOperationType() == OperationType.DEPOSIT) {
                return processDeposit(walletId, request.getAmount());
            } else {
                return processWithdrawal(walletId, request.getAmount());
            }
        } finally {
            Objects.requireNonNull(cacheManager.getCache("wallets")).evict(walletId);
        }
    }

    private WalletResponse processDeposit(UUID walletId, BigDecimal amount) {
        int updated = walletRepository.updateBalance(walletId, amount);

        if (updated == 0) {
            return createNewWallet(walletId, amount);
        }

        BigDecimal newBalance = walletRepository.findBalanceById(walletId)
                .orElseThrow(() -> new IllegalStateException("Баланс кошелька не найден после внесения депозита"));

        log.debug("Внесено {} на кошелек {}. Новый баланс: {}", amount, walletId, newBalance);
        return new WalletResponse(walletId, newBalance);
    }

    private WalletResponse processWithdrawal(UUID walletId, BigDecimal amount) {
        int updated = walletRepository.withdrawWithCheck(walletId, amount);

        if (updated == 0) {
            handleWithdrawalFailure(walletId, amount);
        }

        BigDecimal newBalance = walletRepository.findBalanceById(walletId)
                .orElseThrow(() -> new IllegalStateException("Баланс кошелька не найден после вывода средств"));

        log.debug("Снято {} с кошелька {}. Новый баланс: {}", amount, walletId, newBalance);
        return new WalletResponse(walletId, newBalance);
    }

    private WalletResponse createNewWallet(UUID walletId, BigDecimal amount) {
        Wallet newWallet = Wallet.builder()
                .id(walletId)
                .balance(amount)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        walletRepository.save(newWallet);
        log.info("Создан новый кошелек: {}", walletId);
        return new WalletResponse(walletId, amount);
    }

    private void handleWithdrawalFailure(UUID walletId, BigDecimal amount) {
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException("Кошелёк не найден: " + walletId);
        }

        BigDecimal currentBalance = walletRepository.findBalanceById(walletId)
                .orElse(BigDecimal.ZERO);

        throw new InsufficientFundsException(String.format(
                "Недостаточно средств. Текущий баланс: %s, запрашиваемый: %s",
                currentBalance, amount));
    }

    @Cacheable(value = "wallets", key = "#walletId")
    @Transactional(readOnly = true)
    public WalletResponse getBalance(UUID walletId) {
        BigDecimal balance = walletRepository.findBalanceById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Кошелёк не найден: " + walletId));

        return new WalletResponse(walletId, balance);
    }
}