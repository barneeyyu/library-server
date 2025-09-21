package com.library.service;

import com.library.dto.*;
import com.library.entity.*;
import com.library.exception.*;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowService 單元測試")
@MockitoSettings(strictness = Strictness.LENIENT)
class BorrowServiceTest {

        @Mock
        private BorrowRecordRepository borrowRecordRepository;

        @Mock
        private BookCopyRepository bookCopyRepository;

        @InjectMocks
        private BorrowService borrowService;

        private User memberUser;
        private User librarianUser;
        private Library activeLibrary;
        private Book javaBook;
        private Book pythonMagazine;
        private BookCopy javaBookCopy;
        private BookCopy pythonMagazineCopy;
        private BorrowRecord borrowRecord;
        private BorrowBookRequest borrowRequest;

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

                // 準備圖書館數據
                activeLibrary = new Library();
                activeLibrary.setId(1L);
                activeLibrary.setName("中央圖書館");
                activeLibrary.setAddress("台北市中正區");
                activeLibrary.setActive(true);

                // 準備書籍數據
                javaBook = new Book();
                javaBook.setId(1L);
                javaBook.setTitle("Java程式設計");
                javaBook.setAuthor("張三");
                javaBook.setType(Book.BookType.BOOK);

                pythonMagazine = new Book();
                pythonMagazine.setId(2L);
                pythonMagazine.setTitle("Python期刊");
                pythonMagazine.setAuthor("李四");
                pythonMagazine.setType(Book.BookType.MAGAZINE);

                // 準備書籍副本數據
                javaBookCopy = new BookCopy();
                javaBookCopy.setId(1L);
                javaBookCopy.setBook(javaBook);
                javaBookCopy.setLibrary(activeLibrary);
                javaBookCopy.setTotalCopies(5);
                javaBookCopy.setAvailableCopies(3);
                javaBookCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

                pythonMagazineCopy = new BookCopy();
                pythonMagazineCopy.setId(2L);
                pythonMagazineCopy.setBook(pythonMagazine);
                pythonMagazineCopy.setLibrary(activeLibrary);
                pythonMagazineCopy.setTotalCopies(2);
                pythonMagazineCopy.setAvailableCopies(1);
                pythonMagazineCopy.setStatus(BookCopy.CopyStatus.ACTIVE);

                // 準備借閱記錄
                borrowRecord = new BorrowRecord();
                borrowRecord.setId(1L);
                borrowRecord.setUser(memberUser);
                borrowRecord.setBookCopy(javaBookCopy);
                borrowRecord.setLibrary(activeLibrary); // 設置借閱圖書館
                borrowRecord.setBorrowDate(LocalDate.now());
                borrowRecord.setDueDate(LocalDate.now().plusMonths(1));
                borrowRecord.setStatus(BorrowRecord.BorrowStatus.BORROWED);

                // 準備借書請求
                borrowRequest = new BorrowBookRequest();
                borrowRequest.setBookCopyId(1L);
        }

        @Test
        @DisplayName("成功借書 - 書籍類型")
        void borrowBook_Success_Book() {
                // Given
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.BOOK))
                                .thenReturn(2L);
                when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);
                when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(javaBookCopy);

                // When
                BorrowBookResponse response = borrowService.borrowBook(borrowRequest, memberUser);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getBorrowRecordId()).isEqualTo(1L);
                assertThat(response.getBookTitle()).isEqualTo("Java程式設計");
                assertThat(response.getBookType()).isEqualTo(Book.BookType.BOOK);
                assertThat(response.getStatus()).isEqualTo(BorrowRecord.BorrowStatus.BORROWED);

                // 驗證借閱記錄保存時包含圖書館信息
                verify(borrowRecordRepository).save(argThat(record -> record.getLibrary() != null &&
                                record.getLibrary().getId().equals(1L) &&
                                record.getLibrary().getName().equals("中央圖書館")));
                verify(bookCopyRepository).save(argThat(bookCopy -> bookCopy.getAvailableCopies() == 2)); // 原本3本，借出1本剩2本
        }

        @Test
        @DisplayName("成功借書 - 期刊類型")
        void borrowBook_Success_Magazine() {
                // Given
                borrowRequest.setBookCopyId(2L);
                when(bookCopyRepository.findById(2L)).thenReturn(Optional.of(pythonMagazineCopy));
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.MAGAZINE))
                                .thenReturn(3L);
                when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);
                when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(pythonMagazineCopy);

                // When
                BorrowBookResponse response = borrowService.borrowBook(borrowRequest, memberUser);

                // Then
                assertThat(response).isNotNull();
                verify(borrowRecordRepository).save(any(BorrowRecord.class));
                verify(bookCopyRepository).save(argThat(bookCopy -> bookCopy.getAvailableCopies() == 0)); // 原本1本，借出1本剩0本
        }

        @Test
        @DisplayName("借書成功時正確記錄圖書館信息")
        void borrowBook_RecordsLibraryInformation() {
                // Given
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.BOOK))
                                .thenReturn(0L);
                when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);
                when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(javaBookCopy);

                // When
                BorrowBookResponse response = borrowService.borrowBook(borrowRequest, memberUser);

                // Then
                assertThat(response).isNotNull();

                // 驗證保存的借閱記錄包含正確的圖書館信息
                verify(borrowRecordRepository).save(argThat(record -> {
                        assertThat(record.getLibrary()).isNotNull();
                        assertThat(record.getLibrary().getId()).isEqualTo(activeLibrary.getId());
                        assertThat(record.getLibrary().getName()).isEqualTo(activeLibrary.getName());
                        assertThat(record.getLibrary().getAddress()).isEqualTo(activeLibrary.getAddress());
                        return true;
                }));
        }

        @Test
        @DisplayName("借書失敗：書籍副本不存在")
        void borrowBook_BookCopyNotFound() {
                // Given
                when(bookCopyRepository.findById(999L)).thenReturn(Optional.empty());
                borrowRequest.setBookCopyId(999L);

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("書籍副本不存在：ID 999");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("借書失敗：沒有可借閱的副本")
        void borrowBook_NoAvailableCopies() {
                // Given
                javaBookCopy.setAvailableCopies(0);
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BookNotAvailableException.class)
                                .hasMessage("此書籍副本目前沒有可借閱的數量");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("借書失敗：書籍副本狀態非活躍")
        void borrowBook_BookCopyNotActive() {
                // Given
                javaBookCopy.setStatus(BookCopy.CopyStatus.MAINTENANCE);
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BookNotAvailableException.class)
                                .hasMessage("此書籍副本目前不可借閱");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("借書失敗：圖書館已停用")
        void borrowBook_LibraryInactive() {
                // Given
                activeLibrary.setActive(false);
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BookNotAvailableException.class)
                                .hasMessage("此圖書館目前已停用，無法借閱");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("借書失敗：超過書籍借閱限制")
        void borrowBook_ExceedBookLimit() {
                // Given
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.BOOK))
                                .thenReturn(10L); // 已借10本書籍，達到上限

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BorrowLimitExceededException.class)
                                .hasMessageContaining("您已借閱 10 本書籍，已達到最大借閱數量限制 (10 本)");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("借書失敗：已借閱同一本書")
        void borrowBook_BookAlreadyBorrowed() {
                // Given - 用戶已經借閱了同一本書
                BorrowRecord existingBorrow = new BorrowRecord();
                existingBorrow.setId(2L);
                existingBorrow.setUser(memberUser);
                existingBorrow.setBookCopy(javaBookCopy);
                existingBorrow.setLibrary(activeLibrary);
                existingBorrow.setStatus(BorrowRecord.BorrowStatus.BORROWED);

                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));
                when(borrowRecordRepository.findActiveBorrowByUserAndBook(1L, 1L))
                                .thenReturn(Optional.of(existingBorrow));

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BookAlreadyBorrowedException.class)
                                .hasMessageContaining("您已經借閱了這本書：Java程式設計，每本書同時只能借閱一個副本");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
                verify(bookCopyRepository, never()).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("借書失敗：超過期刊借閱限制")
        void borrowBook_ExceedMagazineLimit() {
                // Given
                borrowRequest.setBookCopyId(2L);
                when(bookCopyRepository.findById(2L)).thenReturn(Optional.of(pythonMagazineCopy));
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.MAGAZINE))
                                .thenReturn(5L); // 已借5本期刊，達到上限

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BorrowLimitExceededException.class)
                                .hasMessageContaining("您已借閱 5 本圖書，已達到最大借閱數量限制 (5 本)");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("成功還書")
        void returnBook_Success() {
                // Given
                when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
                when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);
                when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(javaBookCopy);

                // When
                ReturnBookResponse response = borrowService.returnBook(1L, memberUser);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getBorrowRecordId()).isEqualTo(1L);
                assertThat(response.getStatus()).isEqualTo(BorrowRecord.BorrowStatus.RETURNED);
                assertThat(response.getReturnDate()).isEqualTo(LocalDate.now());

                verify(borrowRecordRepository)
                                .save(argThat(record -> record.getStatus() == BorrowRecord.BorrowStatus.RETURNED &&
                                                record.getReturnDate() != null));
                verify(bookCopyRepository).save(argThat(bookCopy -> bookCopy.getAvailableCopies() == 4)); // 原本3本，還回1本變4本
        }

        @Test
        @DisplayName("還書失敗：借閱記錄不存在")
        void returnBook_RecordNotFound() {
                // Given
                when(borrowRecordRepository.findById(999L)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> borrowService.returnBook(999L, memberUser))
                                .isInstanceOf(BorrowRecordNotFoundException.class)
                                .hasMessage("借閱記錄不存在：ID 999");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("還書失敗：非該用戶的借閱記錄")
        void returnBook_NotUserRecord() {
                // Given
                when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));

                // When & Then
                assertThatThrownBy(() -> borrowService.returnBook(1L, librarianUser))
                                .isInstanceOf(BookNotBorrowedByUserException.class)
                                .hasMessage("您未借閱此書籍");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("還書失敗：書籍已歸還")
        void returnBook_AlreadyReturned() {
                // Given
                borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
                borrowRecord.setReturnDate(LocalDate.now().minusDays(1));
                when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));

                // When & Then
                assertThatThrownBy(() -> borrowService.returnBook(1L, memberUser))
                                .isInstanceOf(BookAlreadyReturnedException.class)
                                .hasMessage("此書籍已歸還");

                verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("查詢用戶借閱歷史")
        void getUserBorrowHistory_Success() {
                // Given
                List<BorrowRecord> borrowRecords = Arrays.asList(borrowRecord);
                when(borrowRecordRepository.findByUserIdWithDetails(1L)).thenReturn(borrowRecords);

                // When
                List<BorrowRecordResponse> responses = borrowService.getUserBorrowHistory(memberUser);

                // Then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getBorrowRecordId()).isEqualTo(1L);
                assertThat(responses.get(0).getBookTitle()).isEqualTo("Java程式設計");

                verify(borrowRecordRepository).findByUserIdWithDetails(1L);
        }

        @Test
        @DisplayName("查詢用戶當前借閱書籍")
        void getCurrentBorrows_Success() {
                // Given
                List<BorrowRecord> borrowRecords = Arrays.asList(borrowRecord);
                when(borrowRecordRepository.findByUserIdAndStatus(1L, BorrowRecord.BorrowStatus.BORROWED))
                                .thenReturn(borrowRecords);

                // When
                List<BorrowRecordResponse> responses = borrowService.getCurrentBorrows(memberUser);

                // Then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getStatus()).isEqualTo(BorrowRecord.BorrowStatus.BORROWED);

                verify(borrowRecordRepository).findByUserIdAndStatus(1L, BorrowRecord.BorrowStatus.BORROWED);
        }

        @Test
        @DisplayName("查詢借閱限制信息")
        void getBorrowLimits_Success() {
                // Given
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.BOOK))
                                .thenReturn(3L);
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(1L, Book.BookType.MAGAZINE))
                                .thenReturn(2L);

                // When
                Map<Book.BookType, BorrowLimitInfo> limits = borrowService.getBorrowLimits(memberUser);

                // Then
                assertThat(limits).hasSize(2);

                BorrowLimitInfo bookLimit = limits.get(Book.BookType.BOOK);
                assertThat(bookLimit.getCurrentCount()).isEqualTo(3);
                assertThat(bookLimit.getMaxLimit()).isEqualTo(10);
                assertThat(bookLimit.getAvailableSlots()).isEqualTo(7);
                assertThat(bookLimit.canBorrow()).isTrue();

                BorrowLimitInfo magazineLimit = limits.get(Book.BookType.MAGAZINE);
                assertThat(magazineLimit.getCurrentCount()).isEqualTo(2);
                assertThat(magazineLimit.getMaxLimit()).isEqualTo(5);
                assertThat(magazineLimit.getAvailableSlots()).isEqualTo(3);
                assertThat(magazineLimit.canBorrow()).isTrue();
        }

        @Test
        @DisplayName("查詢逾期書籍")
        void getOverdueBooks_Success() {
                // Given
                List<BorrowRecord> overdueRecords = Arrays.asList(borrowRecord);
                when(borrowRecordRepository.findOverdueWithDetails(any(LocalDate.class)))
                                .thenReturn(overdueRecords);

                // When
                List<BorrowRecordResponse> responses = borrowService.getOverdueBooks();

                // Then
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getBorrowRecordId()).isEqualTo(1L);

                verify(borrowRecordRepository).findOverdueWithDetails(any(LocalDate.class));
        }

        @Test
        @DisplayName("發送到期通知")
        void sendDueNotifications_Success() {
                // Given
                List<BorrowRecord> dueSoonRecords = Arrays.asList(borrowRecord);
                when(borrowRecordRepository.findDueSoon(any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(dueSoonRecords);

                // When & Then (測試不會拋出異常)
                assertThatCode(() -> borrowService.sendDueNotifications())
                                .doesNotThrowAnyException();

                verify(borrowRecordRepository).findDueSoon(any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("借書時樂觀鎖衝突 - 拋出適當異常")
        void borrowBook_OptimisticLockingFailure_ThrowsBookNotAvailableException() {
                // Given
                when(bookCopyRepository.findById(1L)).thenReturn(Optional.of(javaBookCopy));
                when(borrowRecordRepository.findActiveBorrowByUserAndBook(eq(memberUser.getId()), eq(javaBook.getId())))
                                .thenReturn(Optional.empty());
                when(borrowRecordRepository.countCurrentBorrowsByUserAndBookType(eq(memberUser.getId()), eq(Book.BookType.BOOK)))
                                .thenReturn(0L);
                
                // 模擬樂觀鎖衝突：當嘗試儲存 BookCopy 時拋出異常
                when(bookCopyRepository.save(any(BookCopy.class)))
                                .thenThrow(new OptimisticLockingFailureException("Version conflict"));

                // When & Then
                assertThatThrownBy(() -> borrowService.borrowBook(borrowRequest, memberUser))
                                .isInstanceOf(BookNotAvailableException.class)
                                .hasMessage("書籍借閱失敗，其他用戶同時在借閱此書，請重試");

                verify(bookCopyRepository).findById(1L);
                verify(bookCopyRepository).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("還書時樂觀鎖衝突 - 拋出適當異常")
        void returnBook_OptimisticLockingFailure_ThrowsBookNotAvailableException() {
                // Given
                when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
                when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(borrowRecord);
                
                // 模擬樂觀鎖衝突：當嘗試儲存 BookCopy 時拋出異常
                when(bookCopyRepository.save(any(BookCopy.class)))
                                .thenThrow(new OptimisticLockingFailureException("Version conflict"));

                // When & Then
                assertThatThrownBy(() -> borrowService.returnBook(1L, memberUser))
                                .isInstanceOf(BookNotAvailableException.class)
                                .hasMessage("還書失敗，其他用戶同時在操作此書籍，請重試");

                verify(borrowRecordRepository).findById(1L);
                verify(bookCopyRepository).save(any(BookCopy.class));
        }
}