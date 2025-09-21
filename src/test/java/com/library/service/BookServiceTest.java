package com.library.service;

import com.library.dto.AddBookCopyResponse;
import com.library.dto.BookSearchResponse;
import com.library.dto.CreateBookRequest;
import com.library.dto.CreateBookResponse;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import com.library.entity.User;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.LibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 單元測試")
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceTest {

        @Mock
        private BookRepository bookRepository;

        @Mock
        private BookCopyRepository bookCopyRepository;

        @Mock
        private LibraryRepository libraryRepository;

        @InjectMocks
        private BookService bookService;

        private User librarianUser;
        private User memberUser;
        private Library activeLibrary;
        private Library inactiveLibrary;
        private CreateBookRequest createBookRequest;
        private Book existingBook;
        private BookCopy bookCopy;

        @BeforeEach
        void setUp() {
                // 準備用戶數據
                librarianUser = new User();
                librarianUser.setId(1L);
                librarianUser.setUsername("librarian");
                librarianUser.setRole(User.UserRole.LIBRARIAN);

                memberUser = new User();
                memberUser.setId(2L);
                memberUser.setUsername("member");
                memberUser.setRole(User.UserRole.MEMBER);

                // 準備圖書館數據
                activeLibrary = new Library();
                activeLibrary.setId(1L);
                activeLibrary.setName("中央圖書館");
                activeLibrary.setAddress("台北市中正區");
                activeLibrary.setActive(true);

                inactiveLibrary = new Library();
                inactiveLibrary.setId(2L);
                inactiveLibrary.setName("停用圖書館");
                inactiveLibrary.setActive(false);

                // 準備創建書籍請求
                createBookRequest = new CreateBookRequest();
                createBookRequest.setTitle("Java程式設計");
                createBookRequest.setAuthor("張三");
                createBookRequest.setPublishYear(2023);
                createBookRequest.setType(Book.BookType.BOOK);
                createBookRequest.setIsbn("978-1234567890");
                createBookRequest.setPublisher("技術出版社");

                // 準備書籍數據
                existingBook = new Book();
                existingBook.setId(1L);
                existingBook.setTitle("Java程式設計");
                existingBook.setAuthor("張三");
                existingBook.setPublishYear(2023);
                existingBook.setType(Book.BookType.BOOK);
                existingBook.setIsbn("978-1234567890");
                existingBook.setPublisher("技術出版社");

                // 準備書籍副本數據
                bookCopy = new BookCopy();
                bookCopy.setId(1L);
                bookCopy.setBook(existingBook);
                bookCopy.setLibrary(activeLibrary);
                bookCopy.setTotalCopies(5);
                bookCopy.setAvailableCopies(5);
                bookCopy.setStatus(BookCopy.CopyStatus.ACTIVE);
        }

        @Test
        @DisplayName("館員成功新增書籍 - 新書籍")
        void createBook_NewBook_Success() {
                // Given
                when(bookRepository.findByTitleAndAuthorAndPublishYear("Java程式設計", "張三", 2023))
                                .thenReturn(Optional.empty());
                when(bookRepository.save(any(Book.class))).thenReturn(existingBook);

                // When
                CreateBookResponse response = bookService.createBook(createBookRequest, librarianUser);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getBookId()).isEqualTo(1L);
                assertThat(response.getTitle()).isEqualTo("Java程式設計");
                assertThat(response.getAuthor()).isEqualTo("張三");
                assertThat(response.getPublishYear()).isEqualTo(2023);
                assertThat(response.getType()).isEqualTo(Book.BookType.BOOK);
                assertThat(response.getIsbn()).isEqualTo("978-1234567890");
                assertThat(response.getPublisher()).isEqualTo("技術出版社");

                verify(bookRepository).findByTitleAndAuthorAndPublishYear("Java程式設計", "張三", 2023);
                verify(bookRepository).save(any(Book.class));
                verify(libraryRepository, never()).findById(anyLong());
                verify(bookCopyRepository, never()).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("新增書籍失敗：書籍已存在")
        void createBook_BookAlreadyExists_ThrowsException() {
                // Given
                when(bookRepository.findByTitleAndAuthorAndPublishYear("Java程式設計", "張三", 2023))
                                .thenReturn(Optional.of(existingBook));

                // When & Then
                assertThatThrownBy(() -> bookService.createBook(createBookRequest, librarianUser))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("書籍已存在：《Java程式設計》- 張三 (2023)");

                verify(bookRepository).findByTitleAndAuthorAndPublishYear("Java程式設計", "張三", 2023);
                verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("MEMBER 用戶也可以呼叫 createBook - 權限檢查已移至 Controller 層")
        void createBook_MemberUser_Success() {
                // Given - 現在 Service 層不再檢查權限，專注業務邏輯
                when(bookRepository.findByTitleAndAuthorAndPublishYear("Java程式設計", "張三", 2023))
                                .thenReturn(Optional.empty());
                when(bookRepository.save(any(Book.class))).thenReturn(existingBook);

                // When - MEMBER 用戶也能成功呼叫（權限由 Spring Security 在 Controller 層處理）
                CreateBookResponse response = bookService.createBook(createBookRequest, memberUser);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getBookId()).isEqualTo(1L);
                assertThat(response.getTitle()).isEqualTo("Java程式設計");

                verify(bookRepository).findByTitleAndAuthorAndPublishYear("Java程式設計", "張三", 2023);
                verify(bookRepository).save(any(Book.class));
        }


        @Test
        @DisplayName("根據ID獲取書籍成功")
        void getBookById_Success() {
                // Given
                List<BookCopy> bookCopies = Arrays.asList(bookCopy);
                when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
                when(bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                                Arrays.asList(1L), BookCopy.CopyStatus.ACTIVE, true))
                                .thenReturn(bookCopies);

                // When
                BookSearchResponse response = bookService.getBookById(1L);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(1L);
                assertThat(response.getTitle()).isEqualTo("Java程式設計");
                assertThat(response.getAuthor()).isEqualTo("張三");
                assertThat(response.getLibraries()).hasSize(1);
                assertThat(response.getLibraries().get(0).getLibraryName()).isEqualTo("中央圖書館");

                verify(bookRepository).findById(1L);
                verify(bookCopyRepository).findByBookIdInAndStatusAndLibraryActive(
                                Arrays.asList(1L), BookCopy.CopyStatus.ACTIVE, true);
        }

        @Test
        @DisplayName("根據ID獲取書籍失敗：書籍不存在")
        void getBookById_BookNotFound_ThrowsException() {
                // Given
                when(bookRepository.findById(999L)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> bookService.getBookById(999L))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("書籍不存在：ID 999");

                verify(bookRepository).findById(999L);
                verify(bookCopyRepository, never()).findByBookIdInAndStatusAndLibraryActive(anyList(), any(),
                                anyBoolean());
        }

        @Test
        @DisplayName("搜尋書籍成功")
        void searchBooks_Success() {
                // Given
                List<Book> books = Arrays.asList(existingBook);
                List<BookCopy> bookCopies = Arrays.asList(bookCopy);
                Pageable pageable = PageRequest.of(0, 20);

                when(bookRepository.searchBooks("Java", null, null, pageable)).thenReturn(books);
                when(bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                                Arrays.asList(1L), BookCopy.CopyStatus.ACTIVE, true))
                                .thenReturn(bookCopies);

                // When
                List<BookSearchResponse> responses = bookService.searchBooks("Java", null, null, 0, 20);

                // Then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getTitle()).isEqualTo("Java程式設計");
                assertThat(responses.get(0).getLibraries()).hasSize(1);

                verify(bookRepository).searchBooks("Java", null, null, pageable);
                verify(bookCopyRepository).findByBookIdInAndStatusAndLibraryActive(
                                Arrays.asList(1L), BookCopy.CopyStatus.ACTIVE, true);
        }

        @Test
        @DisplayName("搜尋書籍返回空結果")
        void searchBooks_NoResults() {
                // Given
                Pageable pageable = PageRequest.of(0, 20);
                when(bookRepository.searchBooks("不存在的書", null, null, pageable))
                                .thenReturn(Arrays.asList());

                // When
                List<BookSearchResponse> responses = bookService.searchBooks("不存在的書", null, null, 0, 20);

                // Then
                assertThat(responses).isEmpty();

                verify(bookRepository).searchBooks("不存在的書", null, null, pageable);
                verify(bookCopyRepository, never()).findByBookIdInAndStatusAndLibraryActive(anyList(), any(),
                                anyBoolean());
        }

        @Test
        @DisplayName("搜尋書籍成功 - 多重條件")
        void searchBooks_MultipleConditions_Success() {
                // Given
                List<Book> books = Arrays.asList(existingBook);
                List<BookCopy> bookCopies = Arrays.asList(bookCopy);
                Pageable pageable = PageRequest.of(0, 20);

                when(bookRepository.searchBooks("Java", "張三", 2023, pageable)).thenReturn(books);
                when(bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                                Arrays.asList(1L), BookCopy.CopyStatus.ACTIVE, true))
                                .thenReturn(bookCopies);

                // When
                List<BookSearchResponse> responses = bookService.searchBooks("Java", "張三", 2023, 0, 20);

                // Then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getTitle()).isEqualTo("Java程式設計");
                assertThat(responses.get(0).getAuthor()).isEqualTo("張三");
                assertThat(responses.get(0).getPublishYear()).isEqualTo(2023);

                verify(bookRepository).searchBooks("Java", "張三", 2023, pageable);
                verify(bookCopyRepository).findByBookIdInAndStatusAndLibraryActive(
                                Arrays.asList(1L), BookCopy.CopyStatus.ACTIVE, true);
        }
}