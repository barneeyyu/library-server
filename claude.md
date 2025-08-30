# CLAUDE.md — Best Practices

> 本文件由團隊資深工程師編寫，目的是確保程式碼品質、可測試性與長期維護性。
> 我們相信乾淨、可讀、可測試的程式碼比短期快速交付更重要。

---

## <technical_stack> 技術棧
- 語言/框架：Java 17 + Spring Boot 3.x
- 資料庫：JPA + SQLite
- 單元測試： JUnit + Mockito
- 安全性：
  - 密碼使用 BCrypt
  - JWT (HS256 簽章即可)
  - RBAC：MEMBER, LIBRARIAN

---

## <critical_notes> 關鍵原則
- **以可測試性為首要考量**  
  - 所有業務邏輯必須可被單元測試覆蓋，避免硬編碼依賴（使用介面 / DI 注入）
  - 嚴禁將核心邏輯與 I/O、外部系統強耦合
- **遵循 Clean Code 原則**  
  - 單一職責（SRP）、短函數、語意清晰的命名
  - 移除重複程式碼（DRY）、避免不必要的複雜度（KISS）
  - 提前結束（guard clause）取代深層巢狀 if-else
- **安全優先**  
  - 所有外部輸入必須驗證
  - 憑證、金鑰與敏感資訊絕不進版本控制
- **設計風格**
  - RESTful API 設計
  - Service 層只處理商業邏輯，Controller 只負責接收/回傳 DTO
  - 測試覆蓋率 > 80%
- **文件規範**
  - 所有公開 API 必須有 Swagger、postman腳本 文件
  - 把架構、流程、專案結構、設計理念、取捨等整理成 ARCHITECTURE.md
  - README.md 必須包含專案簡介、安裝指引、使用說明、測試指引
  - 重要邏輯需有內嵌註解說明

---

## <paved_path> 推薦開發流程
1. **需求分析**  
   - 明確定義輸入、輸出與錯誤情境
2. **撰寫測試（TDD 優先）**  
   - 先定義測試案例與期望結果
3. **實作功能**  
   - 保持函數簡短、模組化、可單獨測試
4. **程式碼檢查**  
   - 必須通過 Linter、單元測試與整合測試
5. **Code Review**  
   - 對命名、可讀性、效能與可測試性給予回饋
6. **文件更新**  
   - 更新 README、API 規格與相關架構圖

---

## <patterns> 常見模式與實踐
- **Dependency Injection**：將依賴透過介面傳入以利測試  
- **Repository Pattern**：資料存取邏輯與業務邏輯分離  
- **Strategy Pattern**：針對可替換演算法的業務需求  
- **Factory Function**：建立具預設依賴的物件，避免直接使用 `new` 造成耦合

---