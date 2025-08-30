package com.library.service;

import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.entity.User;
import com.library.exception.LibrarianVerificationException;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ExternalVerificationService externalVerificationService;
    private final AuthenticationManager authenticationManager;
    
    /**
     * 用戶註冊
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String librarianToken) {
        log.info("用戶註冊請求：username={}, role={}", request.getUsername(), request.getRole());
        
        // 驗證用戶名和電子郵件是否已存在
        validateUserNotExists(request.getUsername(), request.getEmail());
        
        // 驗證角色
        User.UserRole userRole = validateAndParseRole(request.getRole());
        
        // 如果是館員，需要進行外部系統驗證
        if (userRole == User.UserRole.LIBRARIAN) {
            validateLibrarianCredentials(librarianToken);
        }
        
        // 創建新用戶
        User user = createUser(request, userRole);
        User savedUser = userRepository.save(user);
        
        // 生成 JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = jwtService.generateToken(userDetails);
        
        log.info("用戶註冊成功：userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return new AuthResponse(token, mapToUserInfo(savedUser));
    }
    
    /**
     * 用戶登入
     */
    public AuthResponse login(LoginRequest request) {
        log.info("用戶登入請求：username={}", request.getUsername());
        
        try {
            // 驗證用戶憑證
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            
            // 載入用戶詳細信息
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("使用者不存在"));
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtService.generateToken(userDetails);
            
            log.info("用戶登入成功：userId={}, username={}", user.getId(), user.getUsername());
            
            return new AuthResponse(token, mapToUserInfo(user));
            
        } catch (BadCredentialsException e) {
            log.warn("用戶登入失敗：username={}, reason={}", request.getUsername(), e.getMessage());
            throw new BadCredentialsException("使用者名稱或密碼錯誤");
        }
    }
    
    /**
     * 驗證用戶名和電子郵件是否已存在
     */
    private void validateUserNotExists(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("使用者名稱已存在：" + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("電子郵件已被使用：" + email);
        }
    }
    
    /**
     * 驗證並解析用戶角色
     */
    private User.UserRole validateAndParseRole(String role) {
        try {
            return User.UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("無效的用戶角色：" + role);
        }
    }
    
    /**
     * 驗證館員憑證
     */
    private void validateLibrarianCredentials(String librarianToken) {
        if (librarianToken == null || librarianToken.trim().isEmpty()) {
            throw new LibrarianVerificationException("館員註冊需要提供 Authorization: todo header");
        }
        
        if (!externalVerificationService.verifyLibrarianCredentials(librarianToken)) {
            throw new LibrarianVerificationException("館員身份驗證失敗，請檢查驗證 token");
        }
    }
    
    /**
     * 創建新用戶
     */
    private User createUser(RegisterRequest request, User.UserRole userRole) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(userRole);
        user.setActive(true);
        
        return user;
    }
    
    /**
     * 將 User 實體轉換為 UserInfo DTO
     */
    private AuthResponse.UserInfo mapToUserInfo(User user) {
        return new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}