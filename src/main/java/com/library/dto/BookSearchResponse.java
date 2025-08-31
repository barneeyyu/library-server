package com.library.dto;

import com.library.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchResponse {
    
    private Long id;
    private String title;
    private String author;
    private Integer publishYear;
    private Book.BookType type;
    private String isbn;
    private String publisher;
    private List<LibraryStockInfo> libraries;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LibraryStockInfo {
        private Long libraryId;
        private String libraryName;
        private String libraryAddress;
        private Integer totalCopies;
        private Integer availableCopies;
        private Boolean isAvailable;
    }
}