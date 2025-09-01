package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.AddBookCopyResponse;
import com.library.dto.BookSearchResponse;
import com.library.dto.CreateBookRequest;
import com.library.dto.CreateBookResponse;
import com.library.entity.Book;
import com.library.entity.User;
import com.library.exception.InsufficientPermissionException;
import com.library.repository.UserRepository;
import com.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(com.library.config.TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("BookController 單元測試")
class BookControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookService bookService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private CreateBookRequest createBookRequest;
        private CreateBookResponse createBookResponse;
        private BookSearchResponse bookSearchResponse;
        private User librarianUser;
        private User memberUser;

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

                // 準備創建書籍請求
                createBookRequest = new CreateBookRequest();
                createBookRequest.setTitle("Java程式設計");
                createBookRequest.setAuthor("張三");
                createBookRequest.setPublishYear(2023);
                createBookRequest.setType(Book.BookType.BOOK);
                createBookRequest.setIsbn("978-1234567890");
                createBookRequest.setPublisher("技術出版社");

                // 準備創建書籍回應
                createBookResponse = new CreateBookResponse(
                                1L, "Java程式設計", "張三", 2023, Book.BookType.BOOK,
                                "978-1234567890", "技術出版社");

                // 準備書籍搜尋回應
                bookSearchResponse = new BookSearchResponse();
                bookSearchResponse.setId(1L);
                bookSearchResponse.setTitle("Java程式設計");
                bookSearchResponse.setAuthor("張三");
                bookSearchResponse.setPublishYear(2023);
                bookSearchResponse.setType(Book.BookType.BOOK);
                bookSearchResponse.setIsbn("978-1234567890");
                bookSearchResponse.setPublisher("技術出版社");

                BookSearchResponse.LibraryStockInfo libraryInfo = new BookSearchResponse.LibraryStockInfo(
                                1L, "中央圖書館", "台北市中正區", 5, 5, true);
                bookSearchResponse.setLibraries(Arrays.asList(libraryInfo));
        }

        @Test
        @DisplayName("館員成功新增書籍")
        @WithMockUser(username = "librarian")
        void createBook_LibrarianSuccess() throws Exception {
                // Given
                when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(librarianUser));
                when(bookService.createBook(any(CreateBookRequest.class), eq(librarianUser)))
                                .thenReturn(createBookResponse);

                // When & Then
                mockMvc.perform(post("/api/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBookRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("書籍新增成功"))
                                .andExpect(jsonPath("$.data.bookId").value(1L))
                                .andExpect(jsonPath("$.data.title").value("Java程式設計"))
                                .andExpect(jsonPath("$.data.author").value("張三"))
                                .andExpect(jsonPath("$.data.publishYear").value(2023))
                                .andExpect(jsonPath("$.data.isbn").value("978-1234567890"))
                                .andExpect(jsonPath("$.data.publisher").value("技術出版社"));

                verify(userRepository).findByUsername("librarian");
                verify(bookService).createBook(any(CreateBookRequest.class), eq(librarianUser));
        }

        @Test
        @DisplayName("新增書籍失敗：權限不足")
        @WithMockUser(username = "member")
        void createBook_InsufficientPermission() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(bookService.createBook(any(CreateBookRequest.class), eq(memberUser)))
                                .thenThrow(new InsufficientPermissionException("只有館員可以新增書籍"));

                // When & Then
                mockMvc.perform(post("/api/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBookRequest)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("只有館員可以新增書籍"));

                verify(userRepository).findByUsername("member");
                verify(bookService).createBook(any(CreateBookRequest.class), eq(memberUser));
        }

        @Test
        @DisplayName("新增書籍失敗：請求參數無效")
        @WithMockUser(username = "librarian")
        void createBook_InvalidRequest() throws Exception {
                // Given
                CreateBookRequest invalidRequest = new CreateBookRequest();
                invalidRequest.setTitle(""); // 無效的書名
                invalidRequest.setAuthor(""); // 無效的作者

                // When & Then
                mockMvc.perform(post("/api/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(bookService, never()).createBook(any(CreateBookRequest.class), any(User.class));
        }

        @Test
        @DisplayName("新增書籍失敗：書籍已存在")
        @WithMockUser(username = "librarian")
        void createBook_BookAlreadyExists() throws Exception {
                // Given
                when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(librarianUser));
                when(bookService.createBook(any(CreateBookRequest.class), eq(librarianUser)))
                                .thenThrow(new IllegalArgumentException("書籍已存在：《Java程式設計》- 張三 (2023)"));

                // When & Then
                mockMvc.perform(post("/api/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBookRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("書籍已存在：《Java程式設計》- 張三 (2023)"));

                verify(userRepository).findByUsername("librarian");
                verify(bookService).createBook(any(CreateBookRequest.class), eq(librarianUser));
        }

        @Test
        @DisplayName("新增書籍失敗：系統錯誤")
        @WithMockUser(username = "librarian")
        void createBook_SystemError() throws Exception {
                // Given
                when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(librarianUser));
                when(bookService.createBook(any(CreateBookRequest.class), eq(librarianUser)))
                                .thenThrow(new RuntimeException("Database connection failed"));

                // When & Then
                mockMvc.perform(post("/api/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createBookRequest)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("書籍新增失敗，請稍後再試"));

                verify(userRepository).findByUsername("librarian");
                verify(bookService).createBook(any(CreateBookRequest.class), eq(librarianUser));
        }

        @Test
        @DisplayName("搜尋書籍成功")
        void searchBooks_Success() throws Exception {
                // Given
                List<BookSearchResponse> searchResults = Arrays.asList(bookSearchResponse);
                when(bookService.searchBooks("Java", null, null, 0, 20))
                                .thenReturn(searchResults);

                // When & Then
                mockMvc.perform(get("/api/books/search")
                                .param("title", "Java")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("找到 1 本書籍"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].title").value("Java程式設計"))
                                .andExpect(jsonPath("$.data[0].author").value("張三"));

                verify(bookService).searchBooks("Java", null, null, 0, 20);
        }

        @Test
        @DisplayName("搜尋書籍成功 - 無結果")
        void searchBooks_NoResults() throws Exception {
                // Given
                when(bookService.searchBooks("不存在的書", null, null, 0, 20))
                                .thenReturn(Arrays.asList());

                // When & Then
                mockMvc.perform(get("/api/books/search")
                                .param("title", "不存在的書")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("未找到符合條件的書籍"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data").isEmpty());

                verify(bookService).searchBooks("不存在的書", null, null, 0, 20);
        }

        @Test
        @DisplayName("搜尋書籍失敗：缺少搜尋條件")
        void searchBooks_MissingSearchCriteria() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/books/search"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("請至少提供一個搜尋條件（書名、作者或年份）"));

                verify(bookService, never()).searchBooks(anyString(), anyString(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("搜尋書籍失敗：無效的分頁參數")
        void searchBooks_InvalidPagination() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/books/search")
                                .param("title", "Java")
                                .param("page", "-1")
                                .param("size", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("頁數不能小於0"));

                verify(bookService, never()).searchBooks(anyString(), anyString(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("搜尋書籍失敗：每頁數量超出限制")
        void searchBooks_SizeExceedsLimit() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/books/search")
                                .param("title", "Java")
                                .param("page", "0")
                                .param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("每頁數量必須在1-100之間"));

                verify(bookService, never()).searchBooks(anyString(), anyString(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("搜尋書籍失敗：系統錯誤")
        void searchBooks_SystemError() throws Exception {
                // Given
                when(bookService.searchBooks("Java", null, null, 0, 20))
                                .thenThrow(new RuntimeException("Database connection failed"));

                // When & Then
                mockMvc.perform(get("/api/books/search")
                                .param("title", "Java")
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("搜尋失敗，請稍後再試"));

                verify(bookService).searchBooks("Java", null, null, 0, 20);
        }

        @Test
        @DisplayName("根據ID獲取書籍成功")
        void getBookById_Success() throws Exception {
                // Given
                when(bookService.getBookById(1L)).thenReturn(bookSearchResponse);

                // When & Then
                mockMvc.perform(get("/api/books/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("書籍資訊獲取成功"))
                                .andExpect(jsonPath("$.data.id").value(1L))
                                .andExpect(jsonPath("$.data.title").value("Java程式設計"))
                                .andExpect(jsonPath("$.data.author").value("張三"));

                verify(bookService).getBookById(1L);
        }

        @Test
        @DisplayName("根據ID獲取書籍失敗：書籍不存在")
        void getBookById_NotFound() throws Exception {
                // Given
                when(bookService.getBookById(999L))
                                .thenThrow(new IllegalArgumentException("書籍不存在：ID 999"));

                // When & Then
                mockMvc.perform(get("/api/books/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("書籍不存在：ID 999"));

                verify(bookService).getBookById(999L);
        }

        @Test
        @DisplayName("根據ID獲取書籍失敗：系統錯誤")
        void getBookById_SystemError() throws Exception {
                // Given
                when(bookService.getBookById(1L))
                                .thenThrow(new RuntimeException("Database connection failed"));

                // When & Then
                mockMvc.perform(get("/api/books/1"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("獲取書籍資訊失敗，請稍後再試"));

                verify(bookService).getBookById(1L);
        }

        @Test
        @DisplayName("缺少書籍ID的請求")
        void getBooksWithoutId() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/books"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("請提供書籍ID"));

                verify(bookService, never()).getBookById(anyLong());
        }
}