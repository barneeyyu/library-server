package com.library.service;

import com.library.dto.BookSearchResponse;
import com.library.dto.CreateBookRequest;
import com.library.dto.CreateBookResponse;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import com.library.entity.User;
import com.library.exception.InsufficientPermissionException;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.LibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {
    
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LibraryRepository libraryRepository;
    
    /**
     * 新增書籍至系統（館員專用）
     */
    @Transactional
    public CreateBookResponse createBook(CreateBookRequest request, User librarian) {
        log.info("館員 {} 新增書籍：{}", librarian.getUsername(), request.getTitle());
        
        // 驗證館員權限
        if (librarian.getRole() != User.UserRole.LIBRARIAN) {
            throw new InsufficientPermissionException("只有館員可以新增書籍");
        }
        
        // 驗證圖書館是否存在
        Library library = libraryRepository.findById(request.getLibraryId())
                .orElseThrow(() -> new IllegalArgumentException("圖書館不存在：ID " + request.getLibraryId()));
        
        if (!library.getActive()) {
            throw new IllegalArgumentException("圖書館已停用，無法新增書籍");
        }
        
        // 檢查是否已存在相同書籍
        Book existingBook = bookRepository.findByTitleAndAuthorAndPublishYear(
                request.getTitle(), request.getAuthor(), request.getPublishYear())
                .orElse(null);
        
        Book book;
        if (existingBook != null) {
            // 書籍已存在，檢查該圖書館是否已有此書
            boolean hasExistingCopy = bookCopyRepository.existsByBookAndLibrary(existingBook, library);
            if (hasExistingCopy) {
                throw new IllegalArgumentException("該圖書館已有此書籍館藏，請使用更新功能增加副本數量");
            }
            book = existingBook;
            log.info("使用現有書籍記錄：{}", book.getId());
        } else {
            // 創建新書籍
            book = createNewBook(request);
            book = bookRepository.save(book);
            log.info("創建新書籍：{}", book.getId());
        }
        
        // 創建書籍副本記錄
        BookCopy bookCopy = createBookCopy(book, library, request.getCopies());
        bookCopy = bookCopyRepository.save(bookCopy);
        
        log.info("新增書籍副本成功：書籍ID={}, 圖書館ID={}, 副本數={}", 
                book.getId(), library.getId(), request.getCopies());
        
        return new CreateBookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublishYear(),
                book.getType(),
                book.getIsbn(),
                book.getPublisher(),
                library.getId(),
                library.getName(),
                bookCopy.getTotalCopies(),
                bookCopy.getAvailableCopies()
        );
    }
    
    /**
     * 根據ID獲取書籍詳細資訊
     */
    public BookSearchResponse getBookById(Long bookId) {
        log.info("獲取書籍詳情：bookId={}", bookId);
        
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍不存在：ID " + bookId));
        
        List<BookCopy> bookCopies = bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                List.of(bookId), BookCopy.CopyStatus.ACTIVE, true);
        
        return buildSearchResponse(book, bookCopies);
    }
    
    /**
     * 搜尋書籍
     */
    public List<BookSearchResponse> searchBooks(String title, String author, Integer year, int page, int size) {
        log.info("搜尋書籍：title={}, author={}, year={}, page={}, size={}", title, author, year, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        List<Book> books = bookRepository.searchBooks(title, author, year, pageable);
        
        if (books.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 獲取所有書籍的副本資訊
        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());
        List<BookCopy> bookCopies = bookCopyRepository.findByBookIdInAndStatusAndLibraryActive(
                bookIds, BookCopy.CopyStatus.ACTIVE, true);
        
        // 按書籍ID分組副本資訊
        Map<Long, List<BookCopy>> copiesByBookId = bookCopies.stream()
                .collect(Collectors.groupingBy(copy -> copy.getBook().getId()));
        
        // 組裝搜尋結果
        return books.stream()
                .map(book -> buildSearchResponse(book, copiesByBookId.get(book.getId())))
                .collect(Collectors.toList());
    }
    
    /**
     * 創建新書籍
     */
    private Book createNewBook(CreateBookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublishYear(request.getPublishYear());
        book.setType(request.getType());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        return book;
    }
    
    /**
     * 創建書籍副本
     */
    private BookCopy createBookCopy(Book book, Library library, Integer copies) {
        BookCopy bookCopy = new BookCopy();
        bookCopy.setBook(book);
        bookCopy.setLibrary(library);
        bookCopy.setTotalCopies(copies);
        bookCopy.setAvailableCopies(copies);
        bookCopy.setStatus(BookCopy.CopyStatus.ACTIVE);
        return bookCopy;
    }
    
    /**
     * 組裝搜尋回應
     */
    private BookSearchResponse buildSearchResponse(Book book, List<BookCopy> bookCopies) {
        BookSearchResponse response = new BookSearchResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPublishYear(book.getPublishYear());
        response.setType(book.getType());
        response.setIsbn(book.getIsbn());
        response.setPublisher(book.getPublisher());
        
        List<BookSearchResponse.LibraryStockInfo> libraryInfos = new ArrayList<>();
        if (bookCopies != null) {
            libraryInfos = bookCopies.stream()
                    .map(copy -> new BookSearchResponse.LibraryStockInfo(
                            copy.getLibrary().getId(),
                            copy.getLibrary().getName(),
                            copy.getLibrary().getAddress(),
                            copy.getTotalCopies(),
                            copy.getAvailableCopies(),
                            copy.isAvailable()
                    ))
                    .collect(Collectors.toList());
        }
        
        response.setLibraries(libraryInfos);
        return response;
    }
}