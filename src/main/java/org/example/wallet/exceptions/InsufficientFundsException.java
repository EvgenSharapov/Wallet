package org.example.wallet.exceptions;

/**
 * Исключение, выбрасываемое при попытке списания средств,
 * когда на счету недостаточно денег
 */
public class InsufficientFundsException extends RuntimeException {
    private final boolean logStackTrace;

    public InsufficientFundsException(String message, boolean logStackTrace) {
        super(message);
        this.logStackTrace = logStackTrace;
    }

    public boolean shouldLogStackTrace() {
        return logStackTrace;
    }
}