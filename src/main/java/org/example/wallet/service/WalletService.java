package org.example.wallet.service;

import org.example.wallet.dto.WalletRequest;
import org.example.wallet.dto.WalletResponse;
import org.example.wallet.exceptions.ConcurrentWalletOperationException;
import org.example.wallet.exceptions.InsufficientFundsException;
import org.example.wallet.exceptions.WalletNotFoundException;
import org.example.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLTransientConnectionException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 5)
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 100, multiplier = 2),
            retryFor = {
                    OptimisticLockingFailureException.class,
                    PessimisticLockingFailureException.class,
                    SQLTransientConnectionException.class
            })
    public WalletResponse processTransaction(WalletRequest request) {
        validateRequest(request);
        UUID walletId = request.getWalletIdAsUUID();

        try {
            return switch (request.getOperationType()) {
                case DEPOSIT -> deposit(walletId, request.getAmount());
                case WITHDRAW -> withdraw(walletId, request.getAmount());
            };
        } catch (WalletNotFoundException | InsufficientFundsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Сбой транзакции для кошелька: {}", walletId, ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public WalletResponse getBalance(UUID walletId) {
        BigDecimal balance = walletRepository.findBalanceById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Кошелёк не найден: " + walletId));
        return new WalletResponse(walletId, balance);
    }

    private WalletResponse deposit(UUID walletId, BigDecimal amount) {
        return walletRepository.depositAmount(walletId, amount)
                .map(newBalance -> {
                    log.info("Пополнил на {} свой кошелек {}. Новый баланс: {}",
                            amount, walletId, newBalance);
                    return new WalletResponse(walletId, newBalance);
                })
                .orElseThrow(() -> new IllegalStateException("Сбой операции по внесению депозита"));
    }

    private WalletResponse withdraw(UUID walletId, BigDecimal amount) {
        BigDecimal currentBalance = walletRepository.findBalanceById(walletId)
                .orElseThrow(() -> {
                    log.warn("Кошелёк не найден: {}", walletId);
                    return new WalletNotFoundException("Кошелёк не найден");
                });

        if (currentBalance.compareTo(amount) < 0) {
            String errorMessage = String.format("Недостаточно средств. Текущий баланс: %.4f, запрашиваемый: %.4f",
                    currentBalance, amount);
            log.warn(errorMessage);
            throw new InsufficientFundsException(errorMessage, false);
        }

        int updated = walletRepository.withdrawAmount(walletId, amount);

        if (updated > 0) {
            BigDecimal newBalance = currentBalance.subtract(amount);
            log.info("Снято {} с кошелька {}. Новый баланс: {}", amount, walletId, newBalance);
            return new WalletResponse(walletId, newBalance);
        }

        throw new ConcurrentWalletOperationException("Обнаружена параллельная операция");
    }

    private void validateRequest(WalletRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
        if (request.getWalletId() == null) {
            throw new IllegalArgumentException("Идентификатор кошелька не должен быть пустым");
        }
    }
}
