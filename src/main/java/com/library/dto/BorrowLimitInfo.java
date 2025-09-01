package com.library.dto;

import com.library.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowLimitInfo {
    
    private Book.BookType bookType;
    private int currentCount;
    private int maxLimit;
    private int availableSlots;
    
    public static BorrowLimitInfo of(Book.BookType bookType, int currentCount) {
        int maxLimit = (bookType == Book.BookType.MAGAZINE) ? 5 : 10;
        int availableSlots = Math.max(0, maxLimit - currentCount);
        
        return new BorrowLimitInfo(bookType, currentCount, maxLimit, availableSlots);
    }
    
    public boolean canBorrow() {
        return availableSlots > 0;
    }
}