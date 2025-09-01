package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.*;
import com.library.entity.*;
import com.library.exception.*;
import com.library.repository.UserRepository;
import com.library.service.BorrowService;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BorrowController.class)
@Import(com.library.config.TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("BorrowController 單元測試")
class BorrowControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BorrowService borrowService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private BorrowBookRequest borrowRequest;
        private BorrowBookResponse borrowResponse;
        private ReturnBookResponse returnResponse;
        private BorrowRecordResponse borrowRecordResponse;
        private User memberUser;
        private User librarianUser;

        @BeforeEach
        void setUp() {
                // 準備用戶數據
                memberUser = new User();
                memberUser.setId(1L);
                memberUser.setUsername("member");
                memberUser.setRole(User.UserRole.MEMBER);

                librarianUser = new User();
                librarianUser.setId(2L);
                librarianUser.setUsername("librarian");
                librarianUser.setRole(User.UserRole.LIBRARIAN);

                // 準備借書請求
                borrowRequest = new BorrowBookRequest();
                borrowRequest.setBookCopyId(1L);

                // 準備借書回應
                borrowResponse = new BorrowBookResponse(
                                1L, 1L, "Java程式設計", "張三", Book.BookType.BOOK,
                                "中央圖書館", LocalDate.now(), LocalDate.now().plusMonths(1),
                                BorrowRecord.BorrowStatus.BORROWED);

                // 準備還書回應
                returnResponse = new ReturnBookResponse(
                                1L, 1L, "Java程式設計", "張三", Book.BookType.BOOK,
                                "中央圖書館", LocalDate.now().minusDays(10),
                                LocalDate.now().plusDays(20), LocalDate.now(),
                                BorrowRecord.BorrowStatus.RETURNED, false);

                // 準備借閱記錄回應
                borrowRecordResponse = new BorrowRecordResponse(
                                1L, 1L, "Java程式設計", "張三", Book.BookType.BOOK,
                                "978-1234567890", "技術出版社", "中央圖書館", "台北市中正區",
                                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20),
                                null, BorrowRecord.BorrowStatus.BORROWED, false, false, 20L);
        }

        @Test
        @DisplayName("成功借書")
        @WithMockUser(username = "member")
        void borrowBook_Success() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.borrowBook(any(BorrowBookRequest.class), eq(memberUser)))
                                .thenReturn(borrowResponse);

                // When & Then
                mockMvc.perform(post("/api/borrows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(borrowRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("借書成功"))
                                .andExpect(jsonPath("$.data.borrowRecordId").value(1L))
                                .andExpect(jsonPath("$.data.bookTitle").value("Java程式設計"))
                                .andExpect(jsonPath("$.data.status").value("BORROWED"));

                verify(borrowService).borrowBook(any(BorrowBookRequest.class), eq(memberUser));
        }

        @Test
        @DisplayName("借書失敗：超過借閱限制")
        @WithMockUser(username = "member")
        void borrowBook_LimitExceeded() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.borrowBook(any(BorrowBookRequest.class), eq(memberUser)))
                                .thenThrow(new BorrowLimitExceededException("您已借閱 10 本書籍，已達到最大借閱數量限制"));

                // When & Then
                mockMvc.perform(post("/api/borrows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(borrowRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("您已借閱 10 本書籍，已達到最大借閱數量限制"));

                verify(borrowService).borrowBook(any(BorrowBookRequest.class), eq(memberUser));
        }

        @Test
        @DisplayName("借書失敗：書籍不可借閱")
        @WithMockUser(username = "member")
        void borrowBook_BookNotAvailable() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.borrowBook(any(BorrowBookRequest.class), eq(memberUser)))
                                .thenThrow(new BookNotAvailableException("此書籍副本目前沒有可借閱的數量"));

                // When & Then
                mockMvc.perform(post("/api/borrows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(borrowRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("此書籍副本目前沒有可借閱的數量"));

                verify(borrowService).borrowBook(any(BorrowBookRequest.class), eq(memberUser));
        }

        @Test
        @DisplayName("借書失敗：請求參數無效")
        @WithMockUser(username = "member")
        void borrowBook_InvalidRequest() throws Exception {
                // Given
                BorrowBookRequest invalidRequest = new BorrowBookRequest();
                // bookCopyId 為 null，違反 @NotNull 驗證

                // When & Then
                mockMvc.perform(post("/api/borrows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(borrowService, never()).borrowBook(any(BorrowBookRequest.class), any(User.class));
        }

        @Test
        @DisplayName("成功還書")
        @WithMockUser(username = "member")
        void returnBook_Success() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.returnBook(1L, memberUser)).thenReturn(returnResponse);

                // When & Then
                mockMvc.perform(put("/api/borrows/1/return")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("還書成功"))
                                .andExpect(jsonPath("$.data.borrowRecordId").value(1L))
                                .andExpect(jsonPath("$.data.status").value("RETURNED"))
                                .andExpect(jsonPath("$.data.wasOverdue").value(false));

                verify(borrowService).returnBook(1L, memberUser);
        }

        @Test
        @DisplayName("還書失敗：借閱記錄不存在")
        @WithMockUser(username = "member")
        void returnBook_RecordNotFound() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.returnBook(999L, memberUser))
                                .thenThrow(new BorrowRecordNotFoundException("借閱記錄不存在：ID 999"));

                // When & Then
                mockMvc.perform(put("/api/borrows/999/return")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(borrowService).returnBook(999L, memberUser);
        }

        @Test
        @DisplayName("還書失敗：非該用戶的借閱記錄")
        @WithMockUser(username = "member")
        void returnBook_NotUserRecord() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.returnBook(1L, memberUser))
                                .thenThrow(new BookNotBorrowedByUserException("您未借閱此書籍"));

                // When & Then
                mockMvc.perform(put("/api/borrows/1/return")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("您未借閱此書籍"));

                verify(borrowService).returnBook(1L, memberUser);
        }

        @Test
        @DisplayName("查詢個人借閱記錄成功")
        @WithMockUser(username = "member")
        void getMyBorrowRecords_Success() throws Exception {
                // Given
                List<BorrowRecordResponse> records = Arrays.asList(borrowRecordResponse);
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.getUserBorrowHistory(memberUser)).thenReturn(records);

                // When & Then
                mockMvc.perform(get("/api/borrows/my-records"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("找到 1 筆借閱記錄"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].borrowRecordId").value(1L));

                verify(borrowService).getUserBorrowHistory(memberUser);
        }

        @Test
        @DisplayName("查詢個人借閱記錄 - 無記錄")
        @WithMockUser(username = "member")
        void getMyBorrowRecords_NoRecords() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.getUserBorrowHistory(memberUser)).thenReturn(Arrays.asList());

                // When & Then
                mockMvc.perform(get("/api/borrows/my-records"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("您目前沒有借閱記錄"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data").isEmpty());

                verify(borrowService).getUserBorrowHistory(memberUser);
        }

        @Test
        @DisplayName("查詢目前借閱中的書籍")
        @WithMockUser(username = "member")
        void getCurrentBorrows_Success() throws Exception {
                // Given
                List<BorrowRecordResponse> records = Arrays.asList(borrowRecordResponse);
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.getCurrentBorrows(memberUser)).thenReturn(records);

                // When & Then
                mockMvc.perform(get("/api/borrows/current"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("您目前借閱了 1 本書籍"))
                                .andExpect(jsonPath("$.data[0].status").value("BORROWED"));

                verify(borrowService).getCurrentBorrows(memberUser);
        }

        @Test
        @DisplayName("查詢借閱限制信息")
        @WithMockUser(username = "member")
        void getBorrowLimits_Success() throws Exception {
                // Given
                Map<Book.BookType, BorrowLimitInfo> limits = new HashMap<>();
                limits.put(Book.BookType.BOOK, BorrowLimitInfo.of(Book.BookType.BOOK, 3));
                limits.put(Book.BookType.MAGAZINE, BorrowLimitInfo.of(Book.BookType.MAGAZINE, 2));

                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
                when(borrowService.getBorrowLimits(memberUser)).thenReturn(limits);

                // When & Then
                mockMvc.perform(get("/api/borrows/limits"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("借閱限制查詢成功"))
                                .andExpect(jsonPath("$.data.BOOK.currentCount").value(3))
                                .andExpect(jsonPath("$.data.BOOK.maxLimit").value(10))
                                .andExpect(jsonPath("$.data.MAGAZINE.currentCount").value(2))
                                .andExpect(jsonPath("$.data.MAGAZINE.maxLimit").value(5));

                verify(borrowService).getBorrowLimits(memberUser);
        }

        @Test
        @DisplayName("館員查詢逾期書籍成功")
        @WithMockUser(username = "librarian")
        void getOverdueBooks_LibrarianSuccess() throws Exception {
                // Given
                List<BorrowRecordResponse> overdueBooks = Arrays.asList(borrowRecordResponse);
                when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(librarianUser));
                when(borrowService.getOverdueBooks()).thenReturn(overdueBooks);

                // When & Then
                mockMvc.perform(get("/api/borrows/overdue"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("找到 1 本逾期書籍"));

                verify(borrowService).getOverdueBooks();
        }

        @Test
        @DisplayName("一般用戶查詢逾期書籍失敗：權限不足")
        @WithMockUser(username = "member")
        void getOverdueBooks_MemberForbidden() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));

                // When & Then
                mockMvc.perform(get("/api/borrows/overdue"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("只有館員可以查詢逾期書籍"));

                verify(borrowService, never()).getOverdueBooks();
        }

        @Test
        @DisplayName("館員發送到期通知成功")
        @WithMockUser(username = "librarian")
        void sendDueNotifications_LibrarianSuccess() throws Exception {
                // Given
                when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(librarianUser));

                // When & Then
                mockMvc.perform(post("/api/borrows/notifications/due-soon"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("到期通知發送完成"));

                verify(borrowService).sendDueNotifications();
        }

        @Test
        @DisplayName("一般用戶發送到期通知失敗：權限不足")
        @WithMockUser(username = "member")
        void sendDueNotifications_MemberForbidden() throws Exception {
                // Given
                when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));

                // When & Then
                mockMvc.perform(post("/api/borrows/notifications/due-soon"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("只有館員可以發送到期通知"));

                verify(borrowService, never()).sendDueNotifications();
        }
}