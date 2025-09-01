package com.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowBookRequest {
    
    @NotNull(message = "書籍副本ID不能為空")
    private Long bookCopyId;
}