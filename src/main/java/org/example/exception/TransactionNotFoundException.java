package org.example.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Integer id) {
        super("Transaction with id " + id + " not found");
    }
}
