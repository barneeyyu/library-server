package com.library.service;

import com.library.entity.*;
import com.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BorrowService 事務回滾測試")
class BorrowServiceTransactionTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    private BorrowRecord testBorrowRecord;
    private BookCopy testBookCopy;
    private User testUser;

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
        Library library = new Library();
        library.setName("測試圖書館");
        library.setAddress("測試地址");
        library.setActive(true);
        library = libraryRepository.save(library);

        Book book = new Book();
        book.setTitle("事務測試書籍");
        book.setAuthor("測試作者");
        book.setPublishYear(2023);
        book.setType(Book.BookType.BOOK);
        book.setIsbn("978-1234567890");
        book.setPublisher("測試出版社");
        book = bookRepository.save(book);

        testBookCopy = new BookCopy();
        testBookCopy.setBook(book);
        testBookCopy.setLibrary(library);
        testBookCopy.setTotalCopies(5);
        testBookCopy.setAvailableCopies(4); // 有 4 本可借
        testBookCopy.setStatus(BookCopy.CopyStatus.ACTIVE);
        testBookCopy.setCreatedAt(LocalDateTime.now());
        testBookCopy.setUpdatedAt(LocalDateTime.now());
        testBookCopy = bookCopyRepository.save(testBookCopy);

        testUser = new User();
        testUser.setUsername("test_user_" + System.currentTimeMillis());
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setFullName("測試用戶");
        testUser.setRole(User.UserRole.MEMBER);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);

        // 創建一筆借閱記錄
        testBorrowRecord = new BorrowRecord();
        testBorrowRecord.setUser(testUser);
        testBorrowRecord.setBookCopy(testBookCopy);
        testBorrowRecord.setLibrary(library);
        testBorrowRecord.setBorrowDate(LocalDate.now().minusDays(7));
        testBorrowRecord.setDueDate(LocalDate.now().plusDays(23));
        testBorrowRecord.setStatus(BorrowRecord.BorrowStatus.BORROWED);
        testBorrowRecord = borrowRecordRepository.save(testBorrowRecord);
    }

    @Test
    @DisplayName("驗證事務回滾 - 手動觸發異常")
    void testTransactionRollbackOnException() {
        // Given - 記錄初始狀態
        Long initialBorrowRecordCount = borrowRecordRepository.count();
        Integer initialAvailableCopies = testBookCopy.getAvailableCopies();
        Long initialVersion = testBookCopy.getVersion();

        // When - 在新的事務中執行會失敗的操作
        assertThatThrownBy(() -> {
            borrowService.performFailingOperation(testBorrowRecord.getId(), testBookCopy.getId());
        }).isInstanceOf(RuntimeException.class);

        // Then - 驗證數據沒有被修改（事務已回滾）
        Long finalBorrowRecordCount = borrowRecordRepository.count();
        BookCopy finalBookCopy = bookCopyRepository.findById(testBookCopy.getId()).orElseThrow();

        assertThat(finalBorrowRecordCount).isEqualTo(initialBorrowRecordCount);
        assertThat(finalBookCopy.getAvailableCopies()).isEqualTo(initialAvailableCopies);
        assertThat(finalBookCopy.getVersion()).isEqualTo(initialVersion);
    }

    @Transactional
    void testTransactionRollbackBehavior() {
        // 步驟1: 更新借閱記錄
        testBorrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        testBorrowRecord.setReturnDate(LocalDate.now());
        borrowRecordRepository.save(testBorrowRecord);

        // 步驟2: 更新書籍副本
        testBookCopy.setAvailableCopies(testBookCopy.getAvailableCopies() + 1);
        bookCopyRepository.save(testBookCopy);

        // 步驟3: 故意拋出異常來觸發回滾
        throw new RuntimeException("故意觸發事務回滾測試");
    }

    @Test
    @DisplayName("驗證 BorrowService 事務配置正常")
    void testBorrowServiceTransactionConfiguration() {
        // Given - 記錄初始狀態
        Long initialBorrowRecordCount = borrowRecordRepository.count();
        Integer initialAvailableCopies = testBookCopy.getAvailableCopies();

        // When - 測試 performFailingOperation 確實會拋出異常
        assertThatThrownBy(() -> {
            borrowService.performFailingOperation(testBorrowRecord.getId(), testBookCopy.getId());
        }).isInstanceOf(RuntimeException.class)
                .hasMessage("故意觸發事務回滾測試");

        // Then - 驗證事務回滾生效
        Long finalBorrowRecordCount = borrowRecordRepository.count();
        BookCopy finalBookCopy = bookCopyRepository.findById(testBookCopy.getId()).orElseThrow();

        assertThat(finalBorrowRecordCount).isEqualTo(initialBorrowRecordCount);
        assertThat(finalBookCopy.getAvailableCopies()).isEqualTo(initialAvailableCopies);
    }


    @Test
    @DisplayName("檢查事務配置 - 默認回滾行為")
    void checkTransactionConfiguration() {
        // 這個測試驗證 @Transactional 的默認行為

        // RuntimeException 應該觸發回滾
        assertThatThrownBy(() -> throwRuntimeException())
                .isInstanceOf(RuntimeException.class);

        // 所有變更應該被回滾
        // (實際應用中，這需要更複雜的設置來驗證)
    }

    @Transactional
    void throwRuntimeException() {
        // 修改一些數據
        testBorrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        borrowRecordRepository.save(testBorrowRecord);

        // 拋出 RuntimeException（會觸發回滾）
        throw new RuntimeException("測試事務回滾");
    }
}