package com.library.dto;

import com.library.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBookCopyResponse {
    
    private Long bookId;
    private String title;
    private String author;
    private Integer publishYear;
    private Book.BookType type;
    private String isbn;
    private String publisher;
    private Long libraryId;
    private String libraryName;
    private Integer totalCopies;
    private Integer availableCopies;
}