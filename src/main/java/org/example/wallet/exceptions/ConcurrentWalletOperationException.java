package org.example.wallet.exceptions;

public class ConcurrentWalletOperationException extends RuntimeException {
    public ConcurrentWalletOperationException(String message) {
        super(message);
    }
}