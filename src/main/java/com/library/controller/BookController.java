package com.library.controller;

import com.library.dto.AddBookCopyRequest;
import com.library.dto.AddBookCopyResponse;
import com.library.dto.ApiResponse;
import com.library.dto.BookSearchResponse;
import com.library.dto.CreateBookRequest;
import com.library.dto.CreateBookResponse;
import com.library.entity.User;
import com.library.exception.InsufficientPermissionException;
import com.library.repository.UserRepository;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "書籍管理", description = "書籍新增、搜尋、副本管理相關 API")
public class BookController {

    private final BookService bookService;
    private final UserRepository userRepository;

    /**
     * 新增書籍（館員專用）
     */
    @Operation(summary = "新增書籍", description = "館員新增書籍基本資訊到系統")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateBookResponse>> createBook(
            @Valid @RequestBody CreateBookRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 獲取當前用戶資訊
            User currentUser = getCurrentUser(userDetails);

            CreateBookResponse response = bookService.createBook(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("書籍新增成功", response));

        } catch (InsufficientPermissionException e) {
            log.warn("權限不足：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("書籍新增失敗：{}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("書籍新增過程中發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("書籍新增失敗，請稍後再試"));
        }
    }

    /**
     * 新增現有書籍副本到不同圖書館（館員專用）
     */
    @Operation(summary = "新增書籍館藏", description = "館員將現有書籍新增到特定圖書館，或增加現有館藏的副本數量")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/copies")
    public ResponseEntity<ApiResponse<AddBookCopyResponse>> addBookCopies(
            @Valid @RequestBody AddBookCopyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 獲取當前用戶資訊
            User currentUser = getCurrentUser(userDetails);

            AddBookCopyResponse response = bookService.addBookCopies(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("書籍副本新增成功", response));

        } catch (InsufficientPermissionException e) {
            log.warn("權限不足：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("書籍副本新增失敗：{}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("書籍副本新增過程中發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("書籍副本新增失敗，請稍後再試"));
        }
    }

    /**
     * 搜尋書籍（公開）
     */
    @Operation(summary = "搜尋書籍", description = "根據書名、作者、年份或圖書館搜尋書籍。至少需要提供一個搜尋條件。")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BookSearchResponse>>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long libraryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 驗證分頁參數
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("頁數不能小於0"));
            }
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("每頁數量必須在1-100之間"));
            }

            // 至少需要一個搜尋條件
            if (title == null && author == null && year == null && libraryId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("請至少提供一個搜尋條件（書名、作者、年份或圖書館）"));
            }

            List<BookSearchResponse> results = bookService.searchBooks(title, author, year, libraryId, page, size);

            String message = results.isEmpty() ? "未找到符合條件的書籍" : String.format("找到 %d 本書籍", results.size());

            return ResponseEntity.ok(ApiResponse.success(message, results));

        } catch (Exception e) {
            log.error("書籍搜尋過程中發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("搜尋失敗，請稍後再試"));
        }
    }

    /**
     * 處理缺少書籍ID的請求
     */
    @Hidden
    @GetMapping("")
    public ResponseEntity<ApiResponse<Void>> getBooksWithoutId() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("請提供書籍ID"));
    }

    /**
     * 獲取書籍詳細資訊（公開）
     */
    @Operation(summary = "獲取書籍詳情", description = "根據書籍ID獲取書籍的詳細資訊，包含所有圖書館的館藏狀況")
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookSearchResponse>> getBookById(@PathVariable Long bookId) {
        try {
            BookSearchResponse book = bookService.getBookById(bookId);
            return ResponseEntity.ok(ApiResponse.success("書籍資訊獲取成功", book));

        } catch (IllegalArgumentException e) {
            log.warn("獲取書籍詳情失敗：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("獲取書籍詳情過程中發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("獲取書籍資訊失敗，請稍後再試"));
        }
    }

    /**
     * 從 UserDetails 獲取 User 實體
     */
    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new InsufficientPermissionException(
                        "JWT token 中的用戶不存在：" + userDetails.getUsername()));
    }
}