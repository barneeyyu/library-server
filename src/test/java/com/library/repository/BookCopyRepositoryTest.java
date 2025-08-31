package com.library.repository;

import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookCopyRepository 單元測試")
class BookCopyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    private Book javaBook;
    private Book pythonBook;
    private Library centralLibrary;
    private Library eastLibrary;
    private Library inactiveLibrary;
    private BookCopy javaCentralCopy;
    private BookCopy javaEastCopy;
    private BookCopy pythonCentralCopy;
    private BookCopy inactiveCopy;

    @BeforeEach
    void setUp() {
        // 準備書籍數據
        javaBook = new Book();
        javaBook.setTitle("Java程式設計");
        javaBook.setAuthor("張三");
        javaBook.setPublishYear(2023);
        javaBook.setType(Book.BookType.BOOK);
        javaBook.setIsbn("978-1234567890");
        javaBook.setPublisher("技術出版社");

        pythonBook = new Book();
        pythonBook.setTitle("Python入門指南");
        pythonBook.setAuthor("李四");
        pythonBook.setPublishYear(2022);
        pythonBook.setType(Book.BookType.BOOK);
        pythonBook.setIsbn("978-0987654321");
        pythonBook.setPublisher("程式出版社");

        // 準備圖書館數據
        centralLibrary = new Library();
        centralLibrary.setName("中央圖書館");
        centralLibrary.setAddress("台北市中正區");
        centralLibrary.setPhone("02-12345678");
        centralLibrary.setActive(true);

        eastLibrary = new Library();
        eastLibrary.setName("東區圖書館");
        eastLibrary.setAddress("台北市信義區");
        eastLibrary.setPhone("02-87654321");
        eastLibrary.setActive(true);

        inactiveLibrary = new Library();
        inactiveLibrary.setName("停用圖書館");
        inactiveLibrary.setAddress("台北市萬華區");
        inactiveLibrary.setPhone("02-11111111");
        inactiveLibrary.setActive(false);

        // 保存實體
        entityManager.persistAndFlush(javaBook);
        entityManager.persistAndFlush(pythonBook);
        entityManager.persistAndFlush(centralLibrary);
        entityManager.persistAndFlush(eastLibrary);
        entityManager.persistAndFlush(inactiveLibrary);

        // 準備書籍副本數據
        javaCentralCopy = new BookCopy();
        javaCentralCopy.setBook(javaBook);
        javaCentralCopy.setLibrary(centralLibrary);
        javaCentralCopy.setTotalCopies(10);
        javaCentralCopy.setAvailableCopies(8);
        javaCentralCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

        javaEastCopy = new BookCopy();
        javaEastCopy.setBook(javaBook);
        javaEastCopy.setLibrary(eastLibrary);
        javaEastCopy.setTotalCopies(5);
        javaEastCopy.setAvailableCopies(0); // 全部借出
        javaEastCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

        pythonCentralCopy = new BookCopy();
        pythonCentralCopy.setBook(pythonBook);
        pythonCentralCopy.setLibrary(centralLibrary);
        pythonCentralCopy.setTotalCopies(3);
        pythonCentralCopy.setAvailableCopies(2);
        pythonCentralCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

        inactiveCopy = new BookCopy();
        inactiveCopy.setBook(javaBook);
        inactiveCopy.setLibrary(inactiveLibrary);
        inactiveCopy.setTotalCopies(2);
        inactiveCopy.setAvailableCopies(2);
        inactiveCopy.setStatus(BookCopy.CopyStatus.MAINTENANCE);

        entityManager.persistAndFlush(javaCentralCopy);
        entityManager.persistAndFlush(javaEastCopy);
        entityManager.persistAndFlush(pythonCentralCopy);
        entityManager.persistAndFlush(inactiveCopy);
    }

    @Test
    @DisplayName("根據書籍ID查找書籍副本")
    void findByBookId_Success() {
        // When
        List<BookCopy> copies = bookCopyRepository.findByBookId(javaBook.getId());

        // Then
        assertThat(copies).hasSize(3); // javaCentralCopy, javaEastCopy, inactiveCopy
        assertThat(copies)
                .extracting(copy -> copy.getBook().getId())
                .containsOnly(javaBook.getId());
    }

    @Test
    @DisplayName("根據圖書館ID查找書籍副本")
    void findByLibraryId_Success() {
        // When
        List<BookCopy> copies = bookCopyRepository.findByLibraryId(centralLibrary.getId());

        // Then
        assertThat(copies).hasSize(2); // javaCentralCopy, pythonCentralCopy
        assertThat(copies)
                .extracting(copy -> copy.getLibrary().getId())
                .containsOnly(centralLibrary.getId());
    }

    @Test
    @DisplayName("查找活躍狀態的書籍副本")
    void findActiveByBookId_Success() {
        // When
        List<BookCopy> copies = bookCopyRepository.findActiveByBookId(javaBook.getId());

        // Then
        assertThat(copies).hasSize(2); // javaCentralCopy, javaEastCopy (排除 maintenance 狀態的)
        assertThat(copies)
                .extracting(BookCopy::getStatus)
                .containsOnly(BookCopy.CopyStatus.ACTIVE);
    }

    @Test
    @DisplayName("查找可借閱的書籍副本")
    void findAvailableByBookId_Success() {
        // When
        List<BookCopy> copies = bookCopyRepository.findAvailableByBookId(javaBook.getId());

        // Then
        assertThat(copies).hasSize(1); // 只有 javaCentralCopy 有可借閱副本
        assertThat(copies.get(0).getAvailableCopies()).isGreaterThan(0);
        assertThat(copies.get(0).getStatus()).isEqualTo(BookCopy.CopyStatus.ACTIVE);
    }

    @Test
    @DisplayName("根據書籍ID列表、狀態和圖書館活躍狀態查找副本")
    void findByBookIdInAndStatusAndLibraryActive_Success() {
        // Given
        List<Long> bookIds = Arrays.asList(javaBook.getId(), pythonBook.getId());

        // When
        List<BookCopy> copies = bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                bookIds, BookCopy.CopyStatus.ACTIVE, true);

        // Then
        assertThat(copies).hasSize(3); // javaCentralCopy, javaEastCopy, pythonCentralCopy
        assertThat(copies)
                .extracting(BookCopy::getStatus)
                .containsOnly(BookCopy.CopyStatus.ACTIVE);
        assertThat(copies)
                .extracting(copy -> copy.getLibrary().getActive())
                .containsOnly(true);
    }

    @Test
    @DisplayName("根據書籍ID列表查找副本 - 排除停用圖書館")
    void findByBookIdInAndStatusAndLibraryActive_ExcludeInactiveLibrary() {
        // Given
        List<Long> bookIds = Arrays.asList(javaBook.getId());

        // When
        List<BookCopy> copies = bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                bookIds, BookCopy.CopyStatus.ACTIVE, true);

        // Then
        assertThat(copies).hasSize(2); // javaCentralCopy, javaEastCopy (排除 inactiveLibrary)
        assertThat(copies)
                .extracting(copy -> copy.getLibrary().getActive())
                .containsOnly(true);
    }

    @Test
    @DisplayName("檢查書籍和圖書館組合是否存在副本")
    void existsByBookAndLibrary_Exists() {
        // When
        boolean exists = bookCopyRepository.existsByBookAndLibrary(javaBook, centralLibrary);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("檢查書籍和圖書館組合是否存在副本 - 不存在")
    void existsByBookAndLibrary_NotExists() {
        // When
        boolean exists = bookCopyRepository.existsByBookAndLibrary(pythonBook, eastLibrary);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("保存書籍副本成功")
    void save_Success() {
        // Given
        BookCopy newCopy = new BookCopy();
        newCopy.setBook(pythonBook);
        newCopy.setLibrary(eastLibrary);
        newCopy.setTotalCopies(7);
        newCopy.setAvailableCopies(7);
        newCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

        // When
        BookCopy savedCopy = bookCopyRepository.save(newCopy);

        // Then
        assertThat(savedCopy.getId()).isNotNull();
        assertThat(savedCopy.getTotalCopies()).isEqualTo(7);
        assertThat(savedCopy.getAvailableCopies()).isEqualTo(7);
        assertThat(savedCopy.getStatus()).isEqualTo(BookCopy.CopyStatus.ACTIVE);

        // 驗證確實保存到資料庫
        List<BookCopy> copies = bookCopyRepository.findByBookId(pythonBook.getId());
        assertThat(copies).hasSize(2); // 原本的 pythonCentralCopy + 新的 newCopy
    }

    @Test
    @DisplayName("更新書籍副本數量")
    void update_AvailableCopies() {
        // Given
        Long copyId = javaCentralCopy.getId();
        int originalAvailable = javaCentralCopy.getAvailableCopies();

        // When
        javaCentralCopy.setAvailableCopies(originalAvailable - 1);
        BookCopy updatedCopy = bookCopyRepository.save(javaCentralCopy);

        // Then
        assertThat(updatedCopy.getAvailableCopies()).isEqualTo(originalAvailable - 1);

        // 驗證確實更新到資料庫
        BookCopy foundCopy = entityManager.find(BookCopy.class, copyId);
        assertThat(foundCopy.getAvailableCopies()).isEqualTo(originalAvailable - 1);
    }

    @Test
    @DisplayName("刪除書籍副本")
    void delete_Success() {
        // Given
        Long copyId = javaCentralCopy.getId();

        // When
        bookCopyRepository.delete(javaCentralCopy);
        entityManager.flush();

        // Then
        BookCopy deletedCopy = entityManager.find(BookCopy.class, copyId);
        assertThat(deletedCopy).isNull();

        // 驗證其他副本仍然存在
        List<BookCopy> remainingCopies = bookCopyRepository.findByBookId(javaBook.getId());
        assertThat(remainingCopies).hasSize(2); // javaEastCopy, inactiveCopy
    }

    @Test
    @DisplayName("統計書籍副本總數")
    void count_Success() {
        // When
        long count = bookCopyRepository.count();

        // Then
        assertThat(count).isEqualTo(4); // javaCentralCopy, javaEastCopy, pythonCentralCopy, inactiveCopy
    }

    @Test
    @DisplayName("根據維護狀態查找副本")
    void findByBookIdInAndStatusAndLibraryActive_MaintenanceStatus() {
        // Given
        List<Long> bookIds = Arrays.asList(javaBook.getId());

        // When
        List<BookCopy> copies = bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                bookIds, BookCopy.CopyStatus.MAINTENANCE, false);

        // Then
        assertThat(copies).hasSize(1); // inactiveCopy
        assertThat(copies.get(0).getStatus()).isEqualTo(BookCopy.CopyStatus.MAINTENANCE);
        assertThat(copies.get(0).getLibrary().getActive()).isFalse();
    }
}