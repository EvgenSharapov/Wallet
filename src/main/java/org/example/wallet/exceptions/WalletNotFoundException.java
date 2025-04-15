package org.example.wallet.exceptions;


/**
 * Исключение, выбрасываемое при попытке доступа к несуществующему кошельку
 */
public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String message) {
        super(message);
    }

}