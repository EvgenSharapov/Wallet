package org.example.wallet.dto;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import org.example.wallet.model.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WalletRequest {
    @NotNull(message = "Идентификатор кошелька обязателен")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            message = "Неверный формат UUID")
    private String walletId;
    @NotNull(message = "Укажите тип операции: DEPOSIT (пополнение) или WITHDRAW (списание)")
    private OperationType operationType;

    @NotNull(message = "Сумма операции обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть положительным числом")
    private BigDecimal amount;

    public UUID getWalletIdAsUUID() {
        try {
            return UUID.fromString(walletId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неверный формат UUID");
        }
    }
}