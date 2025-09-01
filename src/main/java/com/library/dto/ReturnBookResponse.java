package com.library.dto;

import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnBookResponse {
    
    private Long borrowRecordId;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Book.BookType bookType;
    private String libraryName;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private BorrowRecord.BorrowStatus status;
    private boolean wasOverdue;
    
    public static ReturnBookResponse from(BorrowRecord borrowRecord) {
        return new ReturnBookResponse(
                borrowRecord.getId(),
                borrowRecord.getBookCopy().getBook().getId(),
                borrowRecord.getBookCopy().getBook().getTitle(),
                borrowRecord.getBookCopy().getBook().getAuthor(),
                borrowRecord.getBookCopy().getBook().getType(),
                borrowRecord.getBookCopy().getLibrary().getName(),
                borrowRecord.getBorrowDate(),
                borrowRecord.getDueDate(),
                borrowRecord.getReturnDate(),
                borrowRecord.getStatus(),
                borrowRecord.getReturnDate() != null && 
                borrowRecord.getReturnDate().isAfter(borrowRecord.getDueDate())
        );
    }
}