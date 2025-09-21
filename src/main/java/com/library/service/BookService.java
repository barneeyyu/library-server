package com.library.service;

import com.library.dto.AddBookCopyRequest;
import com.library.dto.AddBookCopyResponse;
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
     * 只創建書籍基本資訊，不涉及圖書館副本
     */
    @Transactional
    public CreateBookResponse createBook(CreateBookRequest request, User librarian) {
        log.info("館員 {} 新增書籍：{}", librarian.getUsername(), request.getTitle());
        
        // 權限檢查已由 Spring Security 在 Controller 層處理
        
        // 檢查是否已存在相同書籍
        Book existingBook = bookRepository.findByTitleAndAuthorAndPublishYear(
                request.getTitle(), request.getAuthor(), request.getPublishYear())
                .orElse(null);
        
        if (existingBook != null) {
            throw new IllegalArgumentException("書籍已存在：《" + request.getTitle() + "》- " + request.getAuthor() + " (" + request.getPublishYear() + ")");
        }
        
        // 創建新書籍
        Book book = createNewBook(request);
        book = bookRepository.save(book);
        
        log.info("創建新書籍成功：ID={}, 書名={}", book.getId(), book.getTitle());
        
        return new CreateBookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublishYear(),
                book.getType(),
                book.getIsbn(),
                book.getPublisher()
        );
    }
    
    /**
     * 新增現有書籍副本到不同圖書館（館員專用）
     */
    @Transactional
    public AddBookCopyResponse addBookCopies(AddBookCopyRequest request, User librarian) {
        log.info("館員 {} 新增書籍副本：bookId={}, libraryId={}, copies={}", 
                librarian.getUsername(), request.getBookId(), request.getLibraryId(), request.getCopies());
        
        // 權限檢查已由 Spring Security 在 Controller 層處理
        
        // 驗證書籍是否存在
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("書籍不存在：ID " + request.getBookId()));
        
        // 驗證圖書館是否存在
        Library library = libraryRepository.findById(request.getLibraryId())
                .orElseThrow(() -> new IllegalArgumentException("圖書館不存在：ID " + request.getLibraryId()));
        
        if (!library.getActive()) {
            throw new IllegalArgumentException("圖書館已停用，無法新增書籍副本");
        }
        
        // 檢查該圖書館是否已有此書籍的副本
        BookCopy existingCopy = bookCopyRepository.findByBookAndLibrary(book, library)
                .orElse(null);
        
        BookCopy bookCopy;
        if (existingCopy != null) {
            // 如果已存在副本，增加數量
            existingCopy.setTotalCopies(existingCopy.getTotalCopies() + request.getCopies());
            existingCopy.setAvailableCopies(existingCopy.getAvailableCopies() + request.getCopies());
            bookCopy = bookCopyRepository.save(existingCopy);
            log.info("更新現有書籍副本：總數={}, 可借={}", 
                    bookCopy.getTotalCopies(), bookCopy.getAvailableCopies());
        } else {
            // 如果沒有副本，創建新的副本記錄
            bookCopy = createBookCopy(book, library, request.getCopies());
            bookCopy = bookCopyRepository.save(bookCopy);
            log.info("創建新書籍副本：書籍ID={}, 圖書館ID={}, 副本數={}", 
                    book.getId(), library.getId(), request.getCopies());
        }
        
        return new AddBookCopyResponse(
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
        return searchBooks(title, author, year, null, page, size);
    }
    
    public List<BookSearchResponse> searchBooks(String title, String author, Integer year, Long libraryId, int page, int size) {
        log.info("搜尋書籍：title={}, author={}, year={}, libraryId={}, page={}, size={}", title, author, year, libraryId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        List<Book> books;
        
        if (libraryId != null) {
            books = bookRepository.searchBooksWithLibrary(title, author, year, libraryId, pageable);
        } else {
            books = bookRepository.searchBooks(title, author, year, pageable);
        }
        
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