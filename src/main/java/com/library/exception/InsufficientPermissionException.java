package com.library.exception;

/**
 * 權限不足異常
 * 當用戶已認證但權限不足時拋出，應該返回 403 Forbidden
 */
public class InsufficientPermissionException extends RuntimeException {
    
    public InsufficientPermissionException(String message) {
        super(message);
    }
    
    public InsufficientPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}