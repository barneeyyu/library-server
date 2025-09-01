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
public class BorrowBookResponse {
    
    private Long borrowRecordId;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Book.BookType bookType;
    private String libraryName;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private BorrowRecord.BorrowStatus status;
    
    public static BorrowBookResponse from(BorrowRecord borrowRecord) {
        return new BorrowBookResponse(
                borrowRecord.getId(),
                borrowRecord.getBookCopy().getBook().getId(),
                borrowRecord.getBookCopy().getBook().getTitle(),
                borrowRecord.getBookCopy().getBook().getAuthor(),
                borrowRecord.getBookCopy().getBook().getType(),
                borrowRecord.getLibrary().getName(),
                borrowRecord.getBorrowDate(),
                borrowRecord.getDueDate(),
                borrowRecord.getStatus()
        );
    }
}