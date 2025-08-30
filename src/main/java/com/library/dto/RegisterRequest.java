package com.library.dto;

import com.library.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "使用者名稱不能為空")
    @Size(min = 3, max = 50, message = "使用者名稱長度必須在 3-50 字元之間")
    private String username;
    
    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, max = 100, message = "密碼長度必須在 6-100 字元之間")
    private String password;
    
    @Email(message = "請輸入有效的電子郵件地址")
    @NotBlank(message = "電子郵件不能為空")
    @Size(max = 100, message = "電子郵件長度不能超過 100 字元")
    private String email;
    
    @NotBlank(message = "姓名不能為空")
    @Size(max = 50, message = "姓名長度不能超過 50 字元")
    private String fullName;
    
    @NotBlank(message = "角色不能為空")
    private String role;
}