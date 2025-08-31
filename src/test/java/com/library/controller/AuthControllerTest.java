package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.exception.LibrarianVerificationException;
import com.library.service.AuthService;
import com.library.service.JwtService;
import com.library.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.library.config.TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController 單元測試")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private UserDetailsServiceImpl userDetailsService;

        @Autowired
        private ObjectMapper objectMapper;

        private RegisterRequest memberRegisterRequest;
        private RegisterRequest librarianRegisterRequest;
        private LoginRequest loginRequest;
        private AuthResponse authResponse;

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

                AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                                1L, "testmember", "member@example.com", "測試會員", "MEMBER");
                authResponse = new AuthResponse("jwt-token", userInfo);
        }

        @Test
        @DisplayName("會員註冊成功")
        void register_MemberSuccess() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class), isNull())).thenReturn(authResponse);

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(memberRegisterRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("註冊成功"))
                                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                                .andExpect(jsonPath("$.data.user.username").value("testmember"))
                                .andExpect(jsonPath("$.data.user.role").value("MEMBER"));

                verify(authService).register(any(RegisterRequest.class), isNull());
        }

        @Test
        @DisplayName("館員註冊成功")
        void register_LibrarianSuccess() throws Exception {
                // Given
                AuthResponse.UserInfo librarianUserInfo = new AuthResponse.UserInfo(
                                2L, "testlibrarian", "librarian@example.com", "測試館員", "LIBRARIAN");
                AuthResponse librarianAuthResponse = new AuthResponse("jwt-token", librarianUserInfo);

                when(authService.register(any(RegisterRequest.class), eq("Bearer valid-token")))
                                .thenReturn(librarianAuthResponse);

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(librarianRegisterRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("註冊成功"))
                                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                                .andExpect(jsonPath("$.data.user.username").value("testlibrarian"))
                                .andExpect(jsonPath("$.data.user.role").value("LIBRARIAN"));

                verify(authService).register(any(RegisterRequest.class), eq("Bearer valid-token"));
        }

        @Test
        @DisplayName("註冊失敗：館員驗證失敗")
        void register_LibrarianVerificationFailed() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class), anyString()))
                                .thenThrow(new LibrarianVerificationException("館員身份驗證失敗"));

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .header("Authorization", "Bearer invalid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(librarianRegisterRequest)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("館員身份驗證失敗"));

                verify(authService).register(any(RegisterRequest.class), eq("Bearer invalid-token"));
        }

        @Test
        @DisplayName("註冊失敗：用戶名已存在")
        void register_UsernameExists() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class), isNull()))
                                .thenThrow(new IllegalArgumentException("使用者名稱已存在：testmember"));

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(memberRegisterRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("使用者名稱已存在：testmember"));

                verify(authService).register(any(RegisterRequest.class), isNull());
        }

        @Test
        @DisplayName("註冊失敗：請求驗證錯誤")
        void register_ValidationError() throws Exception {
                // Given
                RegisterRequest invalidRequest = new RegisterRequest();
                invalidRequest.setUsername(""); // 空用戶名
                invalidRequest.setPassword("123"); // 密碼太短
                invalidRequest.setEmail("invalid-email"); // 無效郵箱

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(RegisterRequest.class), any());
        }

        @Test
        @DisplayName("登入成功")
        void login_Success() throws Exception {
                // Given
                when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("登入成功"))
                                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                                .andExpect(jsonPath("$.data.user.username").value("testmember"));

                verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("登入失敗：錯誤的憑證")
        void login_BadCredentials() throws Exception {
                // Given
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new BadCredentialsException("使用者名稱或密碼錯誤"));

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("使用者名稱或密碼錯誤"));

                verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("登入失敗：請求驗證錯誤")
        void login_ValidationError() throws Exception {
                // Given
                LoginRequest invalidRequest = new LoginRequest();
                invalidRequest.setUsername(""); // 空用戶名
                invalidRequest.setPassword(""); // 空密碼

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("獲取當前用戶信息成功")
        @WithMockUser(username = "testmember")
        void getCurrentUser_Success() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/auth/me")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").value("認證狀態正常"));
        }

        @Test
        @DisplayName("獲取當前用戶信息失敗：未認證")
        void getCurrentUser_Unauthorized() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/auth/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").value("認證狀態正常"));
        }

        @Test
        @DisplayName("註冊過程中發生系統錯誤")
        void register_SystemError() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class), isNull()))
                                .thenThrow(new RuntimeException("Database connection failed"));

                // When & Then
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(memberRegisterRequest)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("註冊失敗，請稍後再試"));

                verify(authService).register(any(RegisterRequest.class), isNull());
        }

        @Test
        @DisplayName("登入過程中發生系統錯誤")
        void login_SystemError() throws Exception {
                // Given
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new RuntimeException("Database connection failed"));

                // When & Then
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("登入失敗，請稍後再試"));

                verify(authService).login(any(LoginRequest.class));
        }
}