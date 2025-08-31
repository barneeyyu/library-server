package com.library.service;

import com.library.entity.User;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl 單元測試")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User activeMember;
    private User activeLibrarian;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        // 準備測試數據
        activeMember = new User();
        activeMember.setId(1L);
        activeMember.setUsername("testmember");
        activeMember.setPassword("encodedPassword123");
        activeMember.setEmail("member@example.com");
        activeMember.setFullName("測試會員");
        activeMember.setRole(User.UserRole.MEMBER);
        activeMember.setActive(true);

        activeLibrarian = new User();
        activeLibrarian.setId(2L);
        activeLibrarian.setUsername("testlibrarian");
        activeLibrarian.setPassword("encodedPassword456");
        activeLibrarian.setEmail("librarian@example.com");
        activeLibrarian.setFullName("測試館員");
        activeLibrarian.setRole(User.UserRole.LIBRARIAN);
        activeLibrarian.setActive(true);

        inactiveUser = new User();
        inactiveUser.setId(3L);
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setPassword("encodedPassword789");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setFullName("停用用戶");
        inactiveUser.setRole(User.UserRole.MEMBER);
        inactiveUser.setActive(false);
    }

    @Test
    @DisplayName("載入活躍會員用戶成功")
    void loadUserByUsername_ActiveMember_Success() {
        // Given
        when(userRepository.findByUsername("testmember")).thenReturn(Optional.of(activeMember));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testmember");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testmember");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER");

        // 檢查帳號狀態
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        verify(userRepository).findByUsername("testmember");
    }

    @Test
    @DisplayName("載入活躍館員用戶成功")
    void loadUserByUsername_ActiveLibrarian_Success() {
        // Given
        when(userRepository.findByUsername("testlibrarian")).thenReturn(Optional.of(activeLibrarian));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testlibrarian");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testlibrarian");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword456");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_LIBRARIAN");

        // 檢查帳號狀態
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        verify(userRepository).findByUsername("testlibrarian");
    }

    @Test
    @DisplayName("載入用戶失敗：用戶不存在")
    void loadUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("使用者不存在：nonexistent");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("載入用戶失敗：用戶已停用")
    void loadUserByUsername_UserInactive() {
        // Given
        when(userRepository.findByUsername("inactiveuser")).thenReturn(Optional.of(inactiveUser));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inactiveuser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("使用者帳號已被停用：inactiveuser");

        verify(userRepository).findByUsername("inactiveuser");
    }

    @Test
    @DisplayName("用戶名為 null 時的處理")
    void loadUserByUsername_NullUsername() {
        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByUsername(null);
    }

    @Test
    @DisplayName("用戶名為空字符串時的處理")
    void loadUserByUsername_EmptyUsername() {
        // Given
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("使用者不存在：");

        verify(userRepository).findByUsername("");
    }

    @Test
    @DisplayName("檢查權限格式正確性")
    void loadUserByUsername_AuthorityFormat() {
        // Given
        when(userRepository.findByUsername("testmember")).thenReturn(Optional.of(activeMember));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testmember");

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(1);

        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
        assertThat(authority).isEqualTo("ROLE_MEMBER");
        assertThat(authority).startsWith("ROLE_");

        verify(userRepository).findByUsername("testmember");
    }

    @Test
    @DisplayName("測試不同角色的權限設置")
    void loadUserByUsername_DifferentRoles() {
        // Test MEMBER role
        when(userRepository.findByUsername("testmember")).thenReturn(Optional.of(activeMember));
        UserDetails memberDetails = userDetailsService.loadUserByUsername("testmember");
        assertThat(memberDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER");

        // Test LIBRARIAN role
        when(userRepository.findByUsername("testlibrarian")).thenReturn(Optional.of(activeLibrarian));
        UserDetails librarianDetails = userDetailsService.loadUserByUsername("testlibrarian");
        assertThat(librarianDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_LIBRARIAN");

        verify(userRepository).findByUsername("testmember");
        verify(userRepository).findByUsername("testlibrarian");
    }
}