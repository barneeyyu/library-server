package com.library.config;

import com.library.dto.ApiResponse;
import com.library.exception.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientPermission(InsufficientPermissionException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BorrowLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleBorrowLimitExceeded(BorrowLimitExceededException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BookNotAvailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleBookNotAvailable(BookNotAvailableException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BookNotBorrowedByUserException.class)
    public ResponseEntity<ApiResponse<Object>> handleBookNotBorrowedByUser(BookNotBorrowedByUserException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BookAlreadyReturnedException.class)
    public ResponseEntity<ApiResponse<Object>> handleBookAlreadyReturned(BookAlreadyReturnedException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BorrowRecordNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleBorrowRecordNotFound(BorrowRecordNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BookAlreadyBorrowedException.class)
    public ResponseEntity<ApiResponse<Object>> handleBookAlreadyBorrowed(BookAlreadyBorrowedException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("請求參數驗證失敗"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleJsonParseException(HttpMessageNotReadableException e) {
        String message = "請求格式錯誤";
        
        // 檢查是否是因為變數未設置導致的 JSON 解析錯誤
        if (e.getMessage() != null && e.getMessage().contains("{{")) {
            message = "請求參數中包含未設置的變數，請檢查 Postman 變數設置";
        } else if (e.getCause() instanceof JsonMappingException) {
            message = "JSON 格式錯誤，請檢查請求參數格式";
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系統內部錯誤，請稍後再試"));
    }
}