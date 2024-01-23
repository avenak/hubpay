package com.example.demo.exception;

// Simple exception class for request validation exception.
// Extends RuntimeException so that it will cause transactions to rollback by default.
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
