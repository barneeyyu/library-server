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
public class BorrowRecordResponse {
    
    private Long borrowRecordId;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Book.BookType bookType;
    private String isbn;
    private String publisher;
    private String libraryName;
    private String libraryAddress;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private BorrowRecord.BorrowStatus status;
    private boolean isOverdue;
    private boolean isDueSoon;
    private long daysUntilDue;
    
    public static BorrowRecordResponse from(BorrowRecord borrowRecord) {
        LocalDate today = LocalDate.now();
        long daysUntilDue = 0;
        
        if (borrowRecord.getStatus() == BorrowRecord.BorrowStatus.BORROWED) {
            daysUntilDue = today.until(borrowRecord.getDueDate()).getDays();
        }
        
        return new BorrowRecordResponse(
                borrowRecord.getId(),
                borrowRecord.getBookCopy().getBook().getId(),
                borrowRecord.getBookCopy().getBook().getTitle(),
                borrowRecord.getBookCopy().getBook().getAuthor(),
                borrowRecord.getBookCopy().getBook().getType(),
                borrowRecord.getBookCopy().getBook().getIsbn(),
                borrowRecord.getBookCopy().getBook().getPublisher(),
                borrowRecord.getLibrary().getName(),
                borrowRecord.getLibrary().getAddress(),
                borrowRecord.getBorrowDate(),
                borrowRecord.getDueDate(),
                borrowRecord.getReturnDate(),
                borrowRecord.getStatus(),
                borrowRecord.isOverdue(),
                borrowRecord.isDueSoon(),
                daysUntilDue
        );
    }
}