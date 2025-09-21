package com.library.service;

import com.library.dto.*;
import com.library.entity.*;
import com.library.exception.*;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowRecordRepository;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BorrowService {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    /**
     * 借書功能
     */
    public BorrowBookResponse borrowBook(BorrowBookRequest request, User user) {
        try {
            return performBorrowing(request, user);
        } catch (OptimisticLockingFailureException e) {
            throw new BookNotAvailableException("書籍借閱失敗，其他用戶同時在借閱此書，請重試");
        }
    }

    private BorrowBookResponse performBorrowing(BorrowBookRequest request, User user) {
        // 1. 檢查書籍副本是否存在且可借閱
        BookCopy bookCopy = bookCopyRepository.findById(request.getBookCopyId())
                .orElseThrow(() -> new IllegalArgumentException("書籍副本不存在：ID " + request.getBookCopyId()));

        if (bookCopy.getAvailableCopies() <= 0) {
            throw new BookNotAvailableException("此書籍副本目前沒有可借閱的數量");
        }

        if (bookCopy.getStatus() != BookCopy.CopyStatus.ACTIVE) {
            throw new BookNotAvailableException("此書籍副本目前不可借閱");
        }

        if (!bookCopy.getLibrary().getActive()) {
            throw new BookNotAvailableException("此圖書館目前已停用，無法借閱");
        }

        // 2. 檢查用戶是否已借閱同一本書
        Optional<BorrowRecord> existingBorrow = borrowRecordRepository
                .findActiveBorrowByUserAndBook(user.getId(), bookCopy.getBook().getId());
        if (existingBorrow.isPresent()) {
            throw new BookAlreadyBorrowedException("您已經借閱了這本書：" + bookCopy.getBook().getTitle() +
                    "，每本書同時只能借閱一個副本");
        }

        // 3. 檢查用戶借閱數量限制
        checkBorrowLimits(user.getId(), bookCopy.getBook().getType());

        // 4. 創建借閱記錄（包含圖書館信息）
        BorrowRecord borrowRecord = new BorrowRecord();
        borrowRecord.setUser(user);
        borrowRecord.setBookCopy(bookCopy);
        borrowRecord.setLibrary(bookCopy.getLibrary()); // 記錄借閱圖書館
        borrowRecord.setBorrowDate(LocalDate.now());
        borrowRecord.setDueDate(LocalDate.now().plusMonths(1));
        borrowRecord.setStatus(BorrowRecord.BorrowStatus.BORROWED);

        borrowRecord = borrowRecordRepository.save(borrowRecord);

        // 5. 更新書籍副本可借數量
        bookCopy.setAvailableCopies(bookCopy.getAvailableCopies() - 1);
        bookCopyRepository.save(bookCopy);

        return BorrowBookResponse.from(borrowRecord);
    }

    /**
     * 還書功能
     */
    public ReturnBookResponse returnBook(Long borrowRecordId, User user) {
        try {
            return performReturning(borrowRecordId, user);
        } catch (OptimisticLockingFailureException e) {
            throw new BookNotAvailableException("還書失敗，其他用戶同時在操作此書籍，請重試");
        }
    }

    private ReturnBookResponse performReturning(Long borrowRecordId, User user) {
        // 1. 檢查借閱記錄是否存在
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new BorrowRecordNotFoundException("借閱記錄不存在：ID " + borrowRecordId));

        // 2. 檢查是否為該用戶的借閱記錄
        if (!borrowRecord.getUser().getId().equals(user.getId())) {
            throw new BookNotBorrowedByUserException("您未借閱此書籍");
        }

        // 3. 檢查書籍是否已歸還
        if (borrowRecord.getStatus() == BorrowRecord.BorrowStatus.RETURNED) {
            throw new BookAlreadyReturnedException("此書籍已歸還");
        }

        // 4. 更新借閱記錄
        borrowRecord.setReturnDate(LocalDate.now());
        borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        borrowRecord = borrowRecordRepository.save(borrowRecord);

        // 5. 更新書籍副本可借數量
        BookCopy bookCopy = borrowRecord.getBookCopy();
        bookCopy.setAvailableCopies(bookCopy.getAvailableCopies() + 1);
        bookCopyRepository.save(bookCopy);

        return ReturnBookResponse.from(borrowRecord);
    }

    /**
     * 查詢用戶借閱歷史
     */
    @Transactional(readOnly = true)
    public List<BorrowRecordResponse> getUserBorrowHistory(User user) {
        List<BorrowRecord> borrowRecords = borrowRecordRepository.findByUserIdWithDetails(user.getId());
        return borrowRecords.stream()
                .map(BorrowRecordResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 查詢用戶當前借閱的書籍
     */
    @Transactional(readOnly = true)
    public List<BorrowRecordResponse> getCurrentBorrows(User user) {
        List<BorrowRecord> borrowRecords = borrowRecordRepository.findByUserIdAndStatus(
                user.getId(), BorrowRecord.BorrowStatus.BORROWED);
        return borrowRecords.stream()
                .map(BorrowRecordResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 查詢逾期書籍 (館員專用)
     */
    @Transactional(readOnly = true)
    public List<BorrowRecordResponse> getOverdueBooks() {
        List<BorrowRecord> overdueRecords = borrowRecordRepository.findOverdueWithDetails(LocalDate.now());
        return overdueRecords.stream()
                .map(BorrowRecordResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 查詢用戶借閱限制信息
     */
    @Transactional(readOnly = true)
    public Map<Book.BookType, BorrowLimitInfo> getBorrowLimits(User user) {
        Map<Book.BookType, BorrowLimitInfo> limits = new HashMap<>();

        for (Book.BookType bookType : Book.BookType.values()) {
            long currentCount = borrowRecordRepository.countCurrentBorrowsByUserAndBookType(
                    user.getId(), bookType);
            limits.put(bookType, BorrowLimitInfo.of(bookType, (int) currentCount));
        }

        return limits;
    }

    /**
     * 檢查借閱數量限制
     */
    private void checkBorrowLimits(Long userId, Book.BookType bookType) {
        long currentCount = borrowRecordRepository.countCurrentBorrowsByUserAndBookType(userId, bookType);
        int maxLimit = (bookType == Book.BookType.MAGAZINE) ? 5 : 10;

        if (currentCount >= maxLimit) {
            String bookTypeName = (bookType == Book.BookType.MAGAZINE) ? "圖書" : "書籍";
            throw new BorrowLimitExceededException(
                    String.format("您已借閱 %d 本%s，已達到最大借閱數量限制 (%d 本)",
                            currentCount, bookTypeName, maxLimit));
        }
    }

    /**
     * 發送到期通知 (模擬)
     */
    public void sendDueNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysLater = today.plusDays(5);

        List<BorrowRecord> dueSoonRecords = borrowRecordRepository.findDueSoon(today, fiveDaysLater);

        System.out.println("=== 到期通知發送 ===");
        System.out.println("檢查日期：" + today);
        System.out.println("即將到期書籍數量：" + dueSoonRecords.size());

        for (BorrowRecord record : dueSoonRecords) {
            long daysUntilDue = today.until(record.getDueDate()).getDays();

            System.out.printf("通知發送給用戶：%s (ID: %d)%n",
                    record.getUser().getUsername(), record.getUser().getId());
            System.out.printf("書籍：《%s》 - %s%n",
                    record.getBookCopy().getBook().getTitle(),
                    record.getBookCopy().getBook().getAuthor());
            System.out.printf("圖書館：%s%n", record.getBookCopy().getLibrary().getName());
            System.out.printf("到期日期：%s (%d 天後)%n", record.getDueDate(), daysUntilDue);
            System.out.println("訊息：您借閱的書籍即將到期，請準時歸還。");
            System.out.println("---");
        }

        if (dueSoonRecords.isEmpty()) {
            System.out.println("目前沒有即將到期的書籍。");
        }

        System.out.println("=== 通知發送完成 ===");
    }

    /**
     * 測試專用：模擬事務失敗的操作
     */
    @Transactional
    public void performFailingOperation(Long borrowRecordId, Long bookCopyId) {
        // 步驟1: 更新借閱記錄
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId).orElseThrow();
        borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        borrowRecord.setReturnDate(LocalDate.now());
        borrowRecordRepository.save(borrowRecord);

        // 步驟2: 更新書籍副本
        BookCopy bookCopy = bookCopyRepository.findById(bookCopyId).orElseThrow();
        bookCopy.setAvailableCopies(bookCopy.getAvailableCopies() + 1);
        bookCopyRepository.save(bookCopy);

        // 步驟3: 故意拋出異常來觸發回滾
        throw new RuntimeException("故意觸發事務回滾測試");
    }
}