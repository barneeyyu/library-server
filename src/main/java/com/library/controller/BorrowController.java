package com.library.controller;

import com.library.dto.*;
import com.library.entity.Book;
import com.library.entity.User;
import com.library.exception.*;
import com.library.repository.UserRepository;
import com.library.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/borrows")
@Tag(name = "借閱管理", description = "書籍借閱、歸還、記錄查詢相關 API")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 借書
     */
    @Operation(summary = "借書", description = "會員借閱書籍")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<ApiResponse<BorrowBookResponse>> borrowBook(
            @Valid @RequestBody BorrowBookRequest request,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            BorrowBookResponse response = borrowService.borrowBook(request, user);
            
            return ResponseEntity.ok(ApiResponse.success("借書成功", response));
        } catch (BorrowLimitExceededException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BookNotAvailableException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BookAlreadyBorrowedException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("借書失敗，請稍後再試"));
        }
    }

    /**
     * 還書
     */
    @Operation(summary = "還書", description = "會員歸還已借閱的書籍")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{borrowRecordId}/return")
    public ResponseEntity<ApiResponse<ReturnBookResponse>> returnBook(
            @PathVariable Long borrowRecordId,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            ReturnBookResponse response = borrowService.returnBook(borrowRecordId, user);
            
            return ResponseEntity.ok(ApiResponse.success("還書成功", response));
        } catch (BorrowRecordNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BookNotBorrowedByUserException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (BookAlreadyReturnedException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("還書失敗，請稍後再試"));
        }
    }

    /**
     * 查詢個人借閱記錄
     */
    @Operation(summary = "查詢個人借閱記錄", description = "查詢會員所有借閱歷史記錄")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/my-records")
    public ResponseEntity<ApiResponse<List<BorrowRecordResponse>>> getMyBorrowRecords(
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            List<BorrowRecordResponse> records = borrowService.getUserBorrowHistory(user);
            
            String message = records.isEmpty() ? "您目前沒有借閱記錄" : 
                    String.format("找到 %d 筆借閱記錄", records.size());
            return ResponseEntity.ok(ApiResponse.success(message, records));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查詢借閱記錄失敗，請稍後再試"));
        }
    }

    /**
     * 查詢目前借閱中的書籍
     */
    @Operation(summary = "查詢目前借閱中的書籍", description = "查詢會員目前尚未歸還的書籍")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<BorrowRecordResponse>>> getCurrentBorrows(
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            List<BorrowRecordResponse> records = borrowService.getCurrentBorrows(user);
            
            String message = records.isEmpty() ? "您目前沒有借閱中的書籍" : 
                    String.format("您目前借閱了 %d 本書籍", records.size());
            return ResponseEntity.ok(ApiResponse.success(message, records));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查詢目前借閱記錄失敗，請稍後再試"));
        }
    }

    /**
     * 查詢借閱限制信息
     */
    @Operation(summary = "查詢借閱限制信息", description = "查詢不同書籍類型的借閱限制和當前借閱狀況")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<Map<Book.BookType, BorrowLimitInfo>>> getBorrowLimits(
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Map<Book.BookType, BorrowLimitInfo> limits = borrowService.getBorrowLimits(user);
            
            return ResponseEntity.ok(ApiResponse.success("借閱限制查詢成功", limits));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查詢借閱限制失敗，請稍後再試"));
        }
    }

    /**
     * 查詢逾期書籍 (館員專用)
     */
    @Operation(summary = "查詢逾期書籍", description = "館員查詢所有逾期未歸還的書籍")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<BorrowRecordResponse>>> getOverdueBooks(
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            
            // 檢查是否為館員
            if (user.getRole() != User.UserRole.LIBRARIAN) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("只有館員可以查詢逾期書籍"));
            }
            
            List<BorrowRecordResponse> overdueBooks = borrowService.getOverdueBooks();
            
            String message = overdueBooks.isEmpty() ? "目前沒有逾期書籍" : 
                    String.format("找到 %d 本逾期書籍", overdueBooks.size());
            return ResponseEntity.ok(ApiResponse.success(message, overdueBooks));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查詢逾期書籍失敗，請稍後再試"));
        }
    }

    /**
     * 發送到期通知 (館員專用)
     */
    @Operation(summary = "發送到期通知", description = "館員發送書籍到期歸還提醒通知")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/notifications/due-soon")
    public ResponseEntity<ApiResponse<String>> sendDueNotifications(
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            
            // 檢查是否為館員
            if (user.getRole() != User.UserRole.LIBRARIAN) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("只有館員可以發送到期通知"));
            }
            
            borrowService.sendDueNotifications();
            
            return ResponseEntity.ok(ApiResponse.success("到期通知發送完成", "通知已發送至控制台"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("發送到期通知失敗，請稍後再試"));
        }
    }

    /**
     * 獲取當前用戶
     */
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用戶不存在：" + username);
        }
        return userOpt.get();
    }
}