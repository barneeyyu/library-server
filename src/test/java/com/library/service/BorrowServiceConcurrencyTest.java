package com.library.service;

import com.library.dto.BorrowBookRequest;
import com.library.entity.*;
import com.library.exception.BookNotAvailableException;
import org.springframework.dao.OptimisticLockingFailureException;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BorrowService 併發測試")
class BorrowServiceConcurrencyTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    private Library testLibrary;
    private Book testBook;
    private BookCopy testBookCopy;
    private User user1;
    private User user2;

    @BeforeEach
    @Transactional
    void setUp() {
        // 清理資料
        borrowRecordRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        libraryRepository.deleteAll();
        userRepository.deleteAll();

        // 創建測試資料
        testLibrary = new Library();
        testLibrary.setName("測試圖書館");
        testLibrary.setAddress("測試地址");
        testLibrary.setActive(true);
        testLibrary = libraryRepository.save(testLibrary);

        testBook = new Book();
        testBook.setTitle("併發測試書籍");
        testBook.setAuthor("測試作者");
        testBook.setPublishYear(2023);
        testBook.setType(Book.BookType.BOOK);
        testBook.setIsbn("978-1234567890");
        testBook.setPublisher("測試出版社");
        testBook = bookRepository.save(testBook);

        testBookCopy = new BookCopy();
        testBookCopy.setBook(testBook);
        testBookCopy.setLibrary(testLibrary);
        testBookCopy.setTotalCopies(1);
        testBookCopy.setAvailableCopies(1); // 只有一本可借
        testBookCopy.setStatus(BookCopy.CopyStatus.ACTIVE);
        testBookCopy.setCreatedAt(LocalDateTime.now());
        testBookCopy.setUpdatedAt(LocalDateTime.now());
        testBookCopy = bookCopyRepository.save(testBookCopy);

        // 創建兩個測試用戶 (使用時間戳確保唯一性)
        long timestamp = System.currentTimeMillis();
        
        user1 = new User();
        user1.setUsername("user1_" + timestamp);
        user1.setPassword("password1");
        user1.setEmail("user1_" + timestamp + "@test.com");
        user1.setFullName("測試用戶1");
        user1.setRole(User.UserRole.MEMBER);
        user1.setActive(true);
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("user2_" + timestamp);
        user2.setPassword("password2");
        user2.setEmail("user2_" + timestamp + "@test.com");
        user2.setFullName("測試用戶2");
        user2.setRole(User.UserRole.MEMBER);
        user2.setActive(true);
        user2 = userRepository.save(user2);
    }

    @Test
    @DisplayName("併發借書測試 - 只有一個應該成功")
    void concurrentBorrowingShouldOnlyAllowOne() throws InterruptedException, ExecutionException {
        // Given
        BorrowBookRequest request1 = new BorrowBookRequest();
        request1.setBookCopyId(testBookCopy.getId());

        BorrowBookRequest request2 = new BorrowBookRequest();
        request2.setBookCopyId(testBookCopy.getId());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - 同時發起兩個借書請求
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                borrowService.borrowBook(request1, user1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 捕獲所有異常，包括樂觀鎖衝突轉換後的 BookNotAvailableException
                failureCount.incrementAndGet();
            }
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                borrowService.borrowBook(request2, user2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 捕獲所有異常，包括樂觀鎖衝突轉換後的 BookNotAvailableException
                failureCount.incrementAndGet();
            }
        });

        // 等待兩個請求完成
        CompletableFuture.allOf(future1, future2).get();

        // Then - 驗證結果
        assertThat(successCount.get()).isEqualTo(1);  // 只有一個成功
        assertThat(failureCount.get()).isEqualTo(1);  // 一個失敗

        // 驗證資料庫狀態
        BookCopy updatedBookCopy = bookCopyRepository.findById(testBookCopy.getId()).orElseThrow();
        assertThat(updatedBookCopy.getAvailableCopies()).isEqualTo(0);  // 可借數量為 0

        long borrowRecordCount = borrowRecordRepository.count();
        assertThat(borrowRecordCount).isEqualTo(1);  // 只有一筆借閱記錄
    }

    @Test
    @DisplayName("樂觀鎖版本號測試")
    void optimisticLockVersionTest() {
        // Given - 獲取初始版本號
        BookCopy bookCopy = bookCopyRepository.findById(testBookCopy.getId()).orElseThrow();
        Long initialVersion = bookCopy.getVersion();

        // When - 修改並儲存
        bookCopy.setAvailableCopies(bookCopy.getAvailableCopies() - 1);
        BookCopy savedBookCopy = bookCopyRepository.save(bookCopy);

        // Then - 版本號應該增加
        // 注意：H2 @Version 從 0 開始，第一次更新後變成 1
        if (initialVersion == null || initialVersion == 0L) {
            assertThat(savedBookCopy.getVersion()).isEqualTo(1L);
        } else {
            assertThat(savedBookCopy.getVersion()).isEqualTo(initialVersion + 1);
        }

        // 再次修改
        Long secondVersion = savedBookCopy.getVersion();
        savedBookCopy.setAvailableCopies(savedBookCopy.getAvailableCopies() + 1);
        BookCopy reSavedBookCopy = bookCopyRepository.save(savedBookCopy);

        // 版本號再次增加
        assertThat(reSavedBookCopy.getVersion()).isEqualTo(secondVersion + 1);
    }
}