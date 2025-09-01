package com.library.dto;

import com.library.entity.Book;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateBookRequest {
    
    @NotBlank(message = "書名不能為空")
    @Size(max = 200, message = "書名長度不能超過200字符")
    private String title;
    
    @NotBlank(message = "作者不能為空")
    @Size(max = 100, message = "作者名稱長度不能超過100字符")
    private String author;
    
    @NotNull(message = "出版年份不能為空")
    @Min(value = 1000, message = "出版年份不能小於1000年")
    @Max(value = 9999, message = "出版年份不能大於9999年")
    private Integer publishYear;
    
    @NotNull(message = "書籍類型不能為空")
    private Book.BookType type;
    
    @Size(max = 20, message = "ISBN長度不能超過20字符")
    private String isbn;
    
    @Size(max = 100, message = "出版社名稱長度不能超過100字符")
    private String publisher;
}