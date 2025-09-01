package com.library.dto;

import lombok.Data;

@Data
public class ReturnBookRequest {
    
    // 目前不需要額外參數，借閱記錄ID從URL路徑獲取
    // 可以在未來擴展，例如加入還書狀況評估等
}