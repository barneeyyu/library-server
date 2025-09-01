package com.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBookCopyRequest {
    
    @NotNull(message = "書籍ID不能為空")
    private Long bookId;
    
    @NotNull(message = "圖書館ID不能為空")
    private Long libraryId;
    
    @NotNull(message = "副本數量不能為空")
    @Min(value = 1, message = "副本數量至少為1")
    private Integer copies;
}