package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用戶註冊
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            AuthResponse response = authService.register(request, authorization);
            return ResponseEntity.ok(ApiResponse.success("註冊成功", response));
            
        } catch (IllegalArgumentException e) {
            log.warn("註冊失敗：{}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("註冊過程中發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("註冊失敗，請稍後再試"));
        }
    }
    
    /**
     * 用戶登入
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("登入成功", response));
            
        } catch (BadCredentialsException e) {
            log.warn("登入失敗：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("登入過程中發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("登入失敗，請稍後再試"));
        }
    }
    
    /**
     * 測試認證狀態
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<String>> getCurrentUser() {
        // 這個端點需要認證，如果能訪問到說明 JWT 有效
        return ResponseEntity.ok(ApiResponse.success("認證狀態正常"));
    }
}