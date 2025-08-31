package com.library.service;

import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.entity.User;
import com.library.exception.LibrarianVerificationException;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 單元測試")
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private ExternalVerificationService externalVerificationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest memberRegisterRequest;
    private RegisterRequest librarianRegisterRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // 準備測試數據
        memberRegisterRequest = new RegisterRequest();
        memberRegisterRequest.setUsername("testmember");
        memberRegisterRequest.setPassword("password123");
        memberRegisterRequest.setEmail("member@example.com");
        memberRegisterRequest.setFullName("測試會員");
        memberRegisterRequest.setRole("MEMBER");

        librarianRegisterRequest = new RegisterRequest();
        librarianRegisterRequest.setUsername("testlibrarian");
        librarianRegisterRequest.setPassword("password123");
        librarianRegisterRequest.setEmail("librarian@example.com");
        librarianRegisterRequest.setFullName("測試館員");
        librarianRegisterRequest.setRole("LIBRARIAN");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testmember");
        loginRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testmember");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("member@example.com");
        testUser.setFullName("測試會員");
        testUser.setRole(User.UserRole.MEMBER);
        testUser.setActive(true);

        userDetails = mock(UserDetails.class);
    }

    @Test
    @DisplayName("一般會員註冊成功")
    void register_MemberSuccess() {
        // Given
        when(userRepository.existsByUsername("testmember")).thenReturn(false);
        when(userRepository.existsByEmail("member@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetails.getUsername()).thenReturn("testmember");
        when(userDetailsService.loadUserByUsername("testmember")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(memberRegisterRequest, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUser().getUsername()).isEqualTo("testmember");
        assertThat(response.getUser().getRole()).isEqualTo("MEMBER");

        verify(userRepository).existsByUsername("testmember");
        verify(userRepository).existsByEmail("member@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    @DisplayName("館員註冊成功")
    void register_LibrarianSuccess() {
        // Given
        User librarianUser = new User();
        librarianUser.setId(2L);
        librarianUser.setUsername("testlibrarian");
        librarianUser.setRole(User.UserRole.LIBRARIAN);
        librarianUser.setEmail("librarian@example.com");
        librarianUser.setFullName("測試館員");

        when(userRepository.existsByUsername("testlibrarian")).thenReturn(false);
        when(userRepository.existsByEmail("librarian@example.com")).thenReturn(false);
        when(externalVerificationService.verifyLibrarianCredentials("valid-token")).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(librarianUser);
        when(userDetails.getUsername()).thenReturn("testlibrarian");
        when(userDetailsService.loadUserByUsername("testlibrarian")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(librarianRegisterRequest, "valid-token");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo("testlibrarian");
        assertThat(response.getUser().getRole()).isEqualTo("LIBRARIAN");

        verify(externalVerificationService).verifyLibrarianCredentials("valid-token");
    }

    @Test
    @DisplayName("註冊失敗：用戶名已存在")
    void register_UsernameExists() {
        // Given
        when(userRepository.existsByUsername("testmember")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(memberRegisterRequest, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("使用者名稱已存在：testmember");

        verify(userRepository).existsByUsername("testmember");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("註冊失敗：電子郵件已存在")
    void register_EmailExists() {
        // Given
        when(userRepository.existsByUsername("testmember")).thenReturn(false);
        when(userRepository.existsByEmail("member@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(memberRegisterRequest, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("電子郵件已被使用：member@example.com");

        verify(userRepository).existsByEmail("member@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("註冊失敗：無效的用戶角色")
    void register_InvalidRole() {
        // Given
        memberRegisterRequest.setRole("INVALID_ROLE");

        // When & Then
        assertThatThrownBy(() -> authService.register(memberRegisterRequest, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("無效的用戶角色：INVALID_ROLE");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("館員註冊失敗：缺少驗證 token")
    void register_LibrarianMissingToken() {
        // Given
        when(userRepository.existsByUsername("testlibrarian")).thenReturn(false);
        when(userRepository.existsByEmail("librarian@example.com")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.register(librarianRegisterRequest, null))
                .isInstanceOf(LibrarianVerificationException.class)
                .hasMessage("館員註冊需要提供 Authorization: todo header");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("館員註冊失敗：驗證 token 無效")
    void register_LibrarianInvalidToken() {
        // Given
        when(userRepository.existsByUsername("testlibrarian")).thenReturn(false);
        when(userRepository.existsByEmail("librarian@example.com")).thenReturn(false);
        when(externalVerificationService.verifyLibrarianCredentials("invalid-token")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.register(librarianRegisterRequest, "invalid-token"))
                .isInstanceOf(LibrarianVerificationException.class)
                .hasMessage("館員身份驗證失敗，請檢查驗證 token");

        verify(externalVerificationService).verifyLibrarianCredentials("invalid-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("登入成功")
    void login_Success() {
        // Given
        when(userRepository.findByUsername("testmember")).thenReturn(Optional.of(testUser));
        when(userDetails.getUsername()).thenReturn("testmember");
        when(userDetailsService.loadUserByUsername("testmember")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo("testmember");
        assertThat(response.getUser().getRole()).isEqualTo("MEMBER");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testmember");
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    @DisplayName("登入失敗：錯誤的憑證")
    void login_BadCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("使用者名稱或密碼錯誤");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("登入失敗：用戶不存在")
    void login_UserNotFound() {
        // Given
        when(userRepository.findByUsername("testmember")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("使用者名稱或密碼錯誤");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testmember");
    }
}