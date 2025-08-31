package com.library.repository;

import com.library.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookRepository 單元測試")
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    private Book javaBook;
    private Book pythonBook;
    private Book springBook;

    @BeforeEach
    void setUp() {
        // 準備測試數據
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

        springBook = new Book();
        springBook.setTitle("Spring框架實戰");
        springBook.setAuthor("張三");
        springBook.setPublishYear(2023);
        springBook.setType(Book.BookType.BOOK);
        springBook.setIsbn("978-1111222233");
        springBook.setPublisher("技術出版社");

        entityManager.persistAndFlush(javaBook);
        entityManager.persistAndFlush(pythonBook);
        entityManager.persistAndFlush(springBook);
    }

    @Test
    @DisplayName("根據書名模糊搜尋")
    void findByTitleContainingIgnoreCase_Success() {
        // When
        List<Book> books = bookRepository.findByTitleContainingIgnoreCase("java");

        // Then
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Java程式設計");
    }

    @Test
    @DisplayName("根據書名模糊搜尋 - 大小寫不敏感")
    void findByTitleContainingIgnoreCase_CaseInsensitive() {
        // When
        List<Book> books = bookRepository.findByTitleContainingIgnoreCase("PYTHON");

        // Then
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Python入門指南");
    }

    @Test
    @DisplayName("根據作者模糊搜尋")
    void findByAuthorContainingIgnoreCase_Success() {
        // When
        List<Book> books = bookRepository.findByAuthorContainingIgnoreCase("張");

        // Then
        assertThat(books).hasSize(2);
        assertThat(books)
                .extracting(Book::getAuthor)
                .containsOnly("張三");
    }

    @Test
    @DisplayName("根據出版年份搜尋")
    void findByPublishYear_Success() {
        // When
        List<Book> books = bookRepository.findByPublishYear(2023);

        // Then
        assertThat(books).hasSize(2);
        assertThat(books)
                .extracting(Book::getPublishYear)
                .containsOnly(2023);
    }

    @Test
    @DisplayName("根據書名、作者和出版年份查找唯一書籍")
    void findByTitleAndAuthorAndPublishYear_Found() {
        // When
        Optional<Book> book = bookRepository.findByTitleAndAuthorAndPublishYear(
                "Java程式設計", "張三", 2023);

        // Then
        assertThat(book).isPresent();
        assertThat(book.get().getTitle()).isEqualTo("Java程式設計");
        assertThat(book.get().getAuthor()).isEqualTo("張三");
        assertThat(book.get().getPublishYear()).isEqualTo(2023);
    }

    @Test
    @DisplayName("根據書名、作者和出版年份查找書籍 - 未找到")
    void findByTitleAndAuthorAndPublishYear_NotFound() {
        // When
        Optional<Book> book = bookRepository.findByTitleAndAuthorAndPublishYear(
                "不存在的書", "不存在的作者", 2099);

        // Then
        assertThat(book).isEmpty();
    }

    @Test
    @DisplayName("搜尋書籍 - 只根據書名")
    void searchBooks_ByTitleOnly() {
        // When
        List<Book> books = bookRepository.searchBooks("Java", null, null);

        // Then
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Java程式設計");
    }

    @Test
    @DisplayName("搜尋書籍 - 只根據作者")
    void searchBooks_ByAuthorOnly() {
        // When
        List<Book> books = bookRepository.searchBooks(null, "張三", null);

        // Then
        assertThat(books).hasSize(2);
        assertThat(books)
                .extracting(Book::getAuthor)
                .containsOnly("張三");
    }

    @Test
    @DisplayName("搜尋書籍 - 只根據年份")
    void searchBooks_ByYearOnly() {
        // When
        List<Book> books = bookRepository.searchBooks(null, null, 2022);

        // Then
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getPublishYear()).isEqualTo(2022);
    }

    @Test
    @DisplayName("搜尋書籍 - 多重條件")
    void searchBooks_MultipleConditions() {
        // When
        List<Book> books = bookRepository.searchBooks("Spring", "張三", 2023);

        // Then
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Spring框架實戰");
        assertThat(books.get(0).getAuthor()).isEqualTo("張三");
        assertThat(books.get(0).getPublishYear()).isEqualTo(2023);
    }

    @Test
    @DisplayName("搜尋書籍 - 無結果")
    void searchBooks_NoResults() {
        // When
        List<Book> books = bookRepository.searchBooks("不存在的書", null, null);

        // Then
        assertThat(books).isEmpty();
    }

    @Test
    @DisplayName("搜尋書籍 - 所有條件為空")
    void searchBooks_AllConditionsNull() {
        // When
        List<Book> books = bookRepository.searchBooks(null, null, null);

        // Then
        assertThat(books).hasSize(3); // 應該返回所有書籍
    }

    @Test
    @DisplayName("搜尋書籍 - 帶分頁")
    void searchBooks_WithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<Book> books = bookRepository.searchBooks("程式", null, null, pageable);

        // Then
        assertThat(books).hasSize(1); // 只有 "Java程式設計" 包含 "程式"
    }

    @Test
    @DisplayName("搜尋書籍 - 分頁第二頁")
    void searchBooks_SecondPage() {
        // Given
        Pageable pageable = PageRequest.of(1, 1);

        // When
        List<Book> books = bookRepository.searchBooks(null, "張三", null, pageable);

        // Then
        assertThat(books).hasSize(1); // 第二頁應該有一本書
    }

    @Test
    @DisplayName("保存書籍成功")
    void save_Success() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("新書籍");
        newBook.setAuthor("新作者");
        newBook.setPublishYear(2024);
        newBook.setType(Book.BookType.MAGAZINE);
        newBook.setIsbn("978-9999888877");
        newBook.setPublisher("新出版社");

        // When
        Book savedBook = bookRepository.save(newBook);

        // Then
        assertThat(savedBook.getId()).isNotNull();
        assertThat(savedBook.getTitle()).isEqualTo("新書籍");
        assertThat(savedBook.getAuthor()).isEqualTo("新作者");
        assertThat(savedBook.getType()).isEqualTo(Book.BookType.MAGAZINE);

        // 驗證確實保存到資料庫
        Optional<Book> foundBook = bookRepository.findById(savedBook.getId());
        assertThat(foundBook).isPresent();
        assertThat(foundBook.get().getTitle()).isEqualTo("新書籍");
    }

    @Test
    @DisplayName("根據ID查找書籍")
    void findById_Success() {
        // When
        Optional<Book> book = bookRepository.findById(javaBook.getId());

        // Then
        assertThat(book).isPresent();
        assertThat(book.get().getTitle()).isEqualTo("Java程式設計");
    }

    @Test
    @DisplayName("根據ID查找書籍 - 未找到")
    void findById_NotFound() {
        // When
        Optional<Book> book = bookRepository.findById(999L);

        // Then
        assertThat(book).isEmpty();
    }

    @Test
    @DisplayName("刪除書籍")
    void delete_Success() {
        // Given
        Long bookId = javaBook.getId();

        // When
        bookRepository.delete(javaBook);
        entityManager.flush();

        // Then
        Optional<Book> deletedBook = bookRepository.findById(bookId);
        assertThat(deletedBook).isEmpty();
    }

    @Test
    @DisplayName("統計所有書籍數量")
    void count_Success() {
        // When
        long count = bookRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }
}