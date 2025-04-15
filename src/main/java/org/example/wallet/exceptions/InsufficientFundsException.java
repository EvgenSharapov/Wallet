package org.example.wallet.exceptions;

/**
 * Исключение, выбрасываемое при попытке списания средств,
 * когда на счету недостаточно денег
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

}