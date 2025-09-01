package com.library.exception;

public class BookNotBorrowedByUserException extends RuntimeException {
    public BookNotBorrowedByUserException(String message) {
        super(message);
    }
}