package com.library.repository;

import com.library.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 單元測試")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testMember;
    private User testLibrarian;

    @BeforeEach
    void setUp() {
        // 準備測試數據
        testMember = new User();
        testMember.setUsername("testmember");
        testMember.setPassword("encodedPassword123");
        testMember.setEmail("member@example.com");
        testMember.setFullName("測試會員");
        testMember.setRole(User.UserRole.MEMBER);
        testMember.setActive(true);

        testLibrarian = new User();
        testLibrarian.setUsername("testlibrarian");
        testLibrarian.setPassword("encodedPassword456");
        testLibrarian.setEmail("librarian@example.com");
        testLibrarian.setFullName("測試館員");
        testLibrarian.setRole(User.UserRole.LIBRARIAN);
        testLibrarian.setActive(true);

        // 儲存測試數據
        entityManager.persistAndFlush(testMember);
        entityManager.persistAndFlush(testLibrarian);
    }

    @Test
    @DisplayName("根據用戶名查找用戶 - 成功")
    void findByUsername_Success() {
        // When
        Optional<User> result = userRepository.findByUsername("testmember");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testmember");
        assertThat(result.get().getEmail()).isEqualTo("member@example.com");
        assertThat(result.get().getRole()).isEqualTo(User.UserRole.MEMBER);
    }

    @Test
    @DisplayName("根據用戶名查找用戶 - 不存在")
    void findByUsername_NotFound() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("根據電子郵件查找用戶 - 成功")
    void findByEmail_Success() {
        // When
        Optional<User> result = userRepository.findByEmail("librarian@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testlibrarian");
        assertThat(result.get().getEmail()).isEqualTo("librarian@example.com");
        assertThat(result.get().getRole()).isEqualTo(User.UserRole.LIBRARIAN);
    }

    @Test
    @DisplayName("根據電子郵件查找用戶 - 不存在")
    void findByEmail_NotFound() {
        // When
        Optional<User> result = userRepository.findByEmail("notfound@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("檢查用戶名是否存在 - 存在")
    void existsByUsername_True() {
        // When
        boolean exists = userRepository.existsByUsername("testmember");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("檢查用戶名是否存在 - 不存在")
    void existsByUsername_False() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("檢查電子郵件是否存在 - 存在")
    void existsByEmail_True() {
        // When
        boolean exists = userRepository.existsByEmail("member@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("檢查電子郵件是否存在 - 不存在")
    void existsByEmail_False() {
        // When
        boolean exists = userRepository.existsByEmail("notfound@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("儲存新用戶")
    void save_NewUser() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password");
        newUser.setEmail("newuser@example.com");
        newUser.setFullName("新用戶");
        newUser.setRole(User.UserRole.MEMBER);
        newUser.setActive(true);

        // When
        User savedUser = userRepository.save(newUser);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");

        // 驗證資料庫中確實存在
        Optional<User> foundUser = userRepository.findByUsername("newuser");
        assertThat(foundUser).isPresent();
    }

    @Test
    @DisplayName("更新現有用戶")
    void save_UpdateUser() {
        // Given
        testMember.setFullName("更新後的姓名");
        testMember.setEmail("updated@example.com");

        // When
        User updatedUser = userRepository.save(testMember);

        // Then
        assertThat(updatedUser.getFullName()).isEqualTo("更新後的姓名");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");

        // 驗證資料庫中的數據已更新
        Optional<User> foundUser = userRepository.findByUsername("testmember");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFullName()).isEqualTo("更新後的姓名");
        assertThat(foundUser.get().getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @DisplayName("刪除用戶")
    void delete_User() {
        // Given
        Long userId = testMember.getId();

        // When
        userRepository.delete(testMember);
        entityManager.flush();

        // Then
        Optional<User> foundUser = userRepository.findById(userId);
        assertThat(foundUser).isEmpty();

        boolean exists = userRepository.existsByUsername("testmember");
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("用戶名大小寫敏感測試")
    void findByUsername_CaseSensitive() {
        // When
        Optional<User> result1 = userRepository.findByUsername("TESTMEMBER");
        Optional<User> result2 = userRepository.findByUsername("TestMember");

        // Then
        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();

        // 只有精確匹配才能找到
        Optional<User> result3 = userRepository.findByUsername("testmember");
        assertThat(result3).isPresent();
    }
}