package com.library.repository;

import com.library.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BorrowRecordRepository 單元測試")
class BorrowRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    private User memberUser1;
    private User memberUser2;
    private Library activeLibrary;
    private Book javaBook;
    private Book pythonMagazine;
    private BookCopy javaBookCopy;
    private BookCopy pythonMagazineCopy;
    private BorrowRecord currentBorrow1;
    private BorrowRecord currentBorrow2;
    private BorrowRecord returnedBorrow;
    private BorrowRecord overdueBorrow;

    @BeforeEach
    void setUp() {
        // 準備用戶數據
        memberUser1 = new User();
        memberUser1.setUsername("member1");
        memberUser1.setEmail("member1@test.com");
        memberUser1.setPassword("password");
        memberUser1.setFullName("會員一");
        memberUser1.setRole(User.UserRole.MEMBER);
        memberUser1.setActive(true);

        memberUser2 = new User();
        memberUser2.setUsername("member2");
        memberUser2.setEmail("member2@test.com");
        memberUser2.setPassword("password");
        memberUser2.setFullName("會員二");
        memberUser2.setRole(User.UserRole.MEMBER);
        memberUser2.setActive(true);

        // 準備圖書館數據
        activeLibrary = new Library();
        activeLibrary.setName("中央圖書館");
        activeLibrary.setAddress("台北市中正區");
        activeLibrary.setPhone("02-12345678");
        activeLibrary.setActive(true);

        // 準備書籍數據
        javaBook = new Book();
        javaBook.setTitle("Java程式設計");
        javaBook.setAuthor("張三");
        javaBook.setPublishYear(2023);
        javaBook.setType(Book.BookType.BOOK);
        javaBook.setIsbn("978-1234567890");
        javaBook.setPublisher("技術出版社");

        pythonMagazine = new Book();
        pythonMagazine.setTitle("Python期刊");
        pythonMagazine.setAuthor("李四");
        pythonMagazine.setPublishYear(2023);
        pythonMagazine.setType(Book.BookType.MAGAZINE);
        pythonMagazine.setIsbn("978-0987654321");
        pythonMagazine.setPublisher("科技出版社");

        // 保存實體
        entityManager.persistAndFlush(memberUser1);
        entityManager.persistAndFlush(memberUser2);
        entityManager.persistAndFlush(activeLibrary);
        entityManager.persistAndFlush(javaBook);
        entityManager.persistAndFlush(pythonMagazine);

        // 準備書籍副本數據
        javaBookCopy = new BookCopy();
        javaBookCopy.setBook(javaBook);
        javaBookCopy.setLibrary(activeLibrary);
        javaBookCopy.setTotalCopies(5);
        javaBookCopy.setAvailableCopies(3);
        javaBookCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

        pythonMagazineCopy = new BookCopy();
        pythonMagazineCopy.setBook(pythonMagazine);
        pythonMagazineCopy.setLibrary(activeLibrary);
        pythonMagazineCopy.setTotalCopies(3);
        pythonMagazineCopy.setAvailableCopies(2);
        pythonMagazineCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

        entityManager.persistAndFlush(javaBookCopy);
        entityManager.persistAndFlush(pythonMagazineCopy);

        // 準備借閱記錄數據
        currentBorrow1 = new BorrowRecord();
        currentBorrow1.setUser(memberUser1);
        currentBorrow1.setBookCopy(javaBookCopy);
        currentBorrow1.setLibrary(activeLibrary);
        currentBorrow1.setBorrowDate(LocalDate.now().minusDays(5));
        currentBorrow1.setDueDate(LocalDate.now().plusDays(25));
        currentBorrow1.setStatus(BorrowRecord.BorrowStatus.BORROWED);

        currentBorrow2 = new BorrowRecord();
        currentBorrow2.setUser(memberUser1);
        currentBorrow2.setBookCopy(pythonMagazineCopy);
        currentBorrow2.setLibrary(activeLibrary);
        currentBorrow2.setBorrowDate(LocalDate.now().minusDays(3));
        currentBorrow2.setDueDate(LocalDate.now().plusDays(3)); // 即將到期
        currentBorrow2.setStatus(BorrowRecord.BorrowStatus.BORROWED);

        returnedBorrow = new BorrowRecord();
        returnedBorrow.setUser(memberUser1);
        returnedBorrow.setBookCopy(javaBookCopy);
        returnedBorrow.setLibrary(activeLibrary);
        returnedBorrow.setBorrowDate(LocalDate.now().minusDays(40));
        returnedBorrow.setDueDate(LocalDate.now().minusDays(10));
        returnedBorrow.setReturnDate(LocalDate.now().minusDays(15));
        returnedBorrow.setStatus(BorrowRecord.BorrowStatus.RETURNED);

        overdueBorrow = new BorrowRecord();
        overdueBorrow.setUser(memberUser2);
        overdueBorrow.setBookCopy(javaBookCopy);
        overdueBorrow.setLibrary(activeLibrary);
        overdueBorrow.setBorrowDate(LocalDate.now().minusDays(35));
        overdueBorrow.setDueDate(LocalDate.now().minusDays(5)); // 已逾期
        overdueBorrow.setStatus(BorrowRecord.BorrowStatus.BORROWED);

        entityManager.persistAndFlush(currentBorrow1);
        entityManager.persistAndFlush(currentBorrow2);
        entityManager.persistAndFlush(returnedBorrow);
        entityManager.persistAndFlush(overdueBorrow);
    }

    @Test
    @DisplayName("根據用戶ID查找借閱記錄")
    void findByUserId_Success() {
        // When
        List<BorrowRecord> records = borrowRecordRepository.findByUserId(memberUser1.getId());

        // Then
        assertThat(records).hasSize(3); // currentBorrow1, currentBorrow2, returnedBorrow
        assertThat(records)
                .extracting(record -> record.getUser().getId())
                .containsOnly(memberUser1.getId());
    }

    @Test
    @DisplayName("根據用戶ID和狀態查找借閱記錄")
    void findByUserIdAndStatus_Success() {
        // When
        List<BorrowRecord> records = borrowRecordRepository.findByUserIdAndStatus(
                memberUser1.getId(), BorrowRecord.BorrowStatus.BORROWED);

        // Then
        assertThat(records).hasSize(2); // currentBorrow1, currentBorrow2
        assertThat(records)
                .extracting(BorrowRecord::getStatus)
                .containsOnly(BorrowRecord.BorrowStatus.BORROWED);
    }

    @Test
    @DisplayName("統計用戶當前借閱的書籍數量 - 按書籍類型")
    void countCurrentBorrowsByUserAndBookType_Book() {
        // When
        long count = borrowRecordRepository.countCurrentBorrowsByUserAndBookType(
                memberUser1.getId(), Book.BookType.BOOK);

        // Then
        assertThat(count).isEqualTo(1); // currentBorrow1 (Java書籍)
    }

    @Test
    @DisplayName("統計用戶當前借閱的期刊數量 - 按書籍類型")
    void countCurrentBorrowsByUserAndBookType_Magazine() {
        // When
        long count = borrowRecordRepository.countCurrentBorrowsByUserAndBookType(
                memberUser1.getId(), Book.BookType.MAGAZINE);

        // Then
        assertThat(count).isEqualTo(1); // currentBorrow2 (Python期刊)
    }

    @Test
    @DisplayName("查找即將到期的借閱記錄")
    void findDueSoon_Success() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysLater = today.plusDays(5);

        // When
        List<BorrowRecord> records = borrowRecordRepository.findDueSoon(today, fiveDaysLater);

        // Then
        assertThat(records).hasSize(1); // currentBorrow2 (3天後到期)
        assertThat(records.get(0).getDueDate()).isEqualTo(today.plusDays(3));
        assertThat(records.get(0).getStatus()).isEqualTo(BorrowRecord.BorrowStatus.BORROWED);
    }

    @Test
    @DisplayName("查找逾期的借閱記錄")
    void findOverdue_Success() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        List<BorrowRecord> records = borrowRecordRepository.findOverdue(today);

        // Then
        assertThat(records).hasSize(1); // overdueBorrow
        assertThat(records.get(0).getDueDate()).isBefore(today);
        assertThat(records.get(0).getStatus()).isEqualTo(BorrowRecord.BorrowStatus.BORROWED);
    }

    @Test
    @DisplayName("查找用戶對特定書籍副本的活躍借閱記錄")
    void findActiveBorrowByUserAndBookCopy_Success() {
        // When
        Optional<BorrowRecord> record = borrowRecordRepository.findActiveBorrowByUserAndBookCopy(
                memberUser1.getId(), javaBookCopy.getId());

        // Then
        assertThat(record).isPresent();
        assertThat(record.get().getUser().getId()).isEqualTo(memberUser1.getId());
        assertThat(record.get().getBookCopy().getId()).isEqualTo(javaBookCopy.getId());
        assertThat(record.get().getStatus()).isEqualTo(BorrowRecord.BorrowStatus.BORROWED);
    }

    @Test
    @DisplayName("查找用戶對特定書籍副本的活躍借閱記錄 - 不存在")
    void findActiveBorrowByUserAndBookCopy_NotFound() {
        // When
        Optional<BorrowRecord> record = borrowRecordRepository.findActiveBorrowByUserAndBookCopy(
                memberUser2.getId(), pythonMagazineCopy.getId());

        // Then
        assertThat(record).isEmpty();
    }

    @Test
    @DisplayName("查找用戶借閱記錄並載入詳細信息")
    void findByUserIdWithDetails_Success() {
        // When
        List<BorrowRecord> records = borrowRecordRepository.findByUserIdWithDetails(memberUser1.getId());

        // Then
        assertThat(records).hasSize(3);
        
        // 驗證關聯實體都已載入
        for (BorrowRecord record : records) {
            assertThat(record.getBookCopy()).isNotNull();
            assertThat(record.getBookCopy().getBook()).isNotNull();
            assertThat(record.getBookCopy().getLibrary()).isNotNull();
        }
        
        // 驗證排序（按借閱日期降序）
        assertThat(records.get(0).getBorrowDate()).isAfter(records.get(1).getBorrowDate());
    }

    @Test
    @DisplayName("查找逾期記錄並載入詳細信息")
    void findOverdueWithDetails_Success() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        List<BorrowRecord> records = borrowRecordRepository.findOverdueWithDetails(today);

        // Then
        assertThat(records).hasSize(1);
        
        BorrowRecord record = records.get(0);
        assertThat(record.getUser()).isNotNull();
        assertThat(record.getBookCopy()).isNotNull();
        assertThat(record.getBookCopy().getBook()).isNotNull();
        assertThat(record.getBookCopy().getLibrary()).isNotNull();
        assertThat(record.getDueDate()).isBefore(today);
    }

    @Test
    @DisplayName("保存借閱記錄")
    void save_Success() {
        // Given
        BorrowRecord newRecord = new BorrowRecord();
        newRecord.setUser(memberUser2);
        newRecord.setBookCopy(pythonMagazineCopy);
        newRecord.setLibrary(activeLibrary);
        newRecord.setBorrowDate(LocalDate.now());
        newRecord.setDueDate(LocalDate.now().plusMonths(1));
        newRecord.setStatus(BorrowRecord.BorrowStatus.BORROWED);

        // When
        BorrowRecord savedRecord = borrowRecordRepository.save(newRecord);

        // Then
        assertThat(savedRecord.getId()).isNotNull();
        assertThat(savedRecord.getBorrowDate()).isEqualTo(LocalDate.now());
        assertThat(savedRecord.getDueDate()).isEqualTo(LocalDate.now().plusMonths(1));

        // 驗證確實保存到資料庫
        List<BorrowRecord> allRecords = borrowRecordRepository.findAll();
        assertThat(allRecords).hasSize(5); // 原本4筆 + 新增1筆
    }

    @Test
    @DisplayName("更新借閱記錄狀態")
    void update_Status() {
        // Given
        Long recordId = currentBorrow1.getId();

        // When
        currentBorrow1.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        currentBorrow1.setReturnDate(LocalDate.now());
        BorrowRecord updatedRecord = borrowRecordRepository.save(currentBorrow1);

        // Then
        assertThat(updatedRecord.getStatus()).isEqualTo(BorrowRecord.BorrowStatus.RETURNED);
        assertThat(updatedRecord.getReturnDate()).isEqualTo(LocalDate.now());

        // 驗證確實更新到資料庫
        BorrowRecord foundRecord = entityManager.find(BorrowRecord.class, recordId);
        assertThat(foundRecord.getStatus()).isEqualTo(BorrowRecord.BorrowStatus.RETURNED);
    }

    @Test
    @DisplayName("統計借閱記錄總數")
    void count_Success() {
        // When
        long count = borrowRecordRepository.count();

        // Then
        assertThat(count).isEqualTo(4); // currentBorrow1, currentBorrow2, returnedBorrow, overdueBorrow
    }
}