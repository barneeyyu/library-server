package com.library.exception;

/**
 * 館員身份驗證失敗異常
 * 當館員驗證失敗時拋出，應該返回 403 Forbidden
 */
public class LibrarianVerificationException extends RuntimeException {
    
    public LibrarianVerificationException(String message) {
        super(message);
    }
    
    public LibrarianVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}