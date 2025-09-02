# 線上圖書借閱系統

基於 Java + Spring Boot 開發的多館圖書借閱管理系統。

> **⚠️ 首次使用須知**  
> 本專案需要 `.env` 環境變數檔案才能正常運行。請先複製 `.env.example` 為 `.env` 並根據需求調整配置。詳見 [安裝指引](#安裝指引)。

## 專案簡介

本系統提供完整的圖書館管理功能，支援多館系統、使用者權限管理、書籍借閱等核心功能。設計遵循 Clean Code 原則，具備高可測試性與可維護性。

## 快速開始

### 環境需求
- Java 17+
- Maven 3.6+

### 安裝指引

1. **複製專案**
   ```bash
   git clone <repository-url>
   cd library-server
   ```

2. **設定環境變數**
   
   ⚠️ **重要**: 應用程式依賴 `.env` 檔案提供敏感配置，必須先建立此檔案才能正常運行。
   
   ```bash
   # 將 .env 檔案放到專案根目錄
   cp .env.example .env
   # 或直接建立 .env 檔案
   ```
   記得先將`.env`裡面的參數正確配置後系統才能正常運行
   
   > **安全提醒**: `.env` 檔案包含敏感資訊，已加入 `.gitignore`，不會被提交到版本控制。

3. **編譯專案**
   ```bash
   mvn clean compile
   ```

4. **啟動應用**
   ```bash
   mvn spring-boot:run
   ```

## 使用說明

### API 端點

系統提供完整的 RESTful API，並整合 Swagger 文檔以便於開發和測試。

#### Swagger API 文檔
- **Swagger UI**：http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**：http://localhost:8080/v3/api-docs

#### 主要 API 端點

##### 🔐 認證管理 (`/api/auth`)
- `POST /api/auth/register` - 用戶註冊（支援會員和館員）
- `POST /api/auth/login` - 用戶登入
- `GET /api/auth/me` - 驗證當前認證狀態

##### 📚 書籍管理 (`/api/books`)
- `POST /api/books` - 新增書籍（館員專用）
- `POST /api/books/copies` - 新增書籍副本到圖書館（館員專用）
- `GET /api/books/search` - 搜尋書籍（公開）
- `GET /api/books/{bookId}` - 獲取書籍詳細資訊（公開）

##### 📖 借閱管理 (`/api/borrows`)
- `POST /api/borrows` - 借書
- `PUT /api/borrows/{borrowRecordId}/return` - 還書
- `GET /api/borrows/my-records` - 查詢個人借閱記錄
- `GET /api/borrows/current` - 查詢目前借閱中的書籍
- `GET /api/borrows/limits` - 查詢借閱限制信息
- `GET /api/borrows/overdue` - 查詢逾期書籍（館員專用）
- `POST /api/borrows/notifications/due-soon` - 發送到期通知（館員專用）

#### 認證方式
大部分 API 需要 JWT 認證，請在請求標頭中加入：
```
Authorization: Bearer {{YOUR_JWT_TOKEN}}
```

### Postman 測試 (最推)

專案包含完整的 Postman 測試集合：
- 匯入 [`postman/Library-Management-System.postman_collection.json`](postman/Library-Management-System.postman_collection.json)
- 詳細說明請見 [postman/README.md](postman/README.md)

### 資料庫
- **H2 資料庫**位於 `./data/library.mv.db`
- 每次啟動會重建資料表（開發模式）
- **H2 Console 訪問**：http://localhost:8080/h2-console

#### H2 Console 連線設定：
- **JDBC URL**: `jdbc:h2:file:./data/library`
- **User Name**: `sa`
- **Password**: `password`
- **Driver Class**: `org.h2.Driver`

## 測試指引

### 單元測試
```bash
mvn test
```

### 整合測試
```bash
mvn verify
```

### 測試覆蓋率
```bash
mvn jacoco:report
```
執行後，報告位於 `target/site/jacoco/index.html`。
目標測試覆蓋率：> 80%

## 開發工具設定

### IDE 需求
- 安裝 Lombok 插件
- 啟用 Annotation Processing

### 除錯模式
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 文件

- [架構設計](ARCHITECTURE.md) - 系統架構、設計決策、技術選型
- [開發規範](CLAUDE.md) - 編碼標準、最佳實踐、開發流程

## 開發指南

### 查看資料庫
1. **H2 Console**（推薦）：
   - 啟動應用程式後訪問 http://localhost:8080/h2-console
   - 使用上述連線設定登入

2. **檢查資料庫檔案**：
   ```bash
   ls -la ./data/
   # 會看到 library.mv.db 檔案
   ```

### 常見問題

1. **應用程式啟動失敗**：
   - 確認 `.env` 檔案是否存在於專案根目錄
   - 檢查 `.env` 檔案中的配置是否正確
   - 確認 `JWT_SECRET` 長度至少 32 字元

2. **無法連線 H2 Console**：
   - 確認應用程式正在運行
   - 檢查 URL：http://localhost:8080/h2-console
   - 確認 `.env` 中的 `SERVER_PORT` 配置

3. **資料庫鎖定問題**：
   - 確保只有一個應用程式實例在運行
   - 重啟應用程式會重建資料庫

4. **JWT Token 過期**：
   - Token 有效期為 24 小時（可在 `.env` 中調整 `JWT_EXPIRATION`）
   - 重新登入獲取新 Token

5. **館員註冊失敗**：
   - 檢查 `.env` 中的 `EXTERNAL_VERIFICATION_URL` 是否正確
   - 確認網路連線可以訪問外部驗證服務

## 貢獻指南

1. 遵循 [CLAUDE.md](CLAUDE.md) 中的開發規範
2. 撰寫測試優先（TDD）
3. 確保測試覆蓋率 > 80%
4. 所有 PR 必須通過 Code Review

## 授權

此專案為考題實作，僅供學習參考使用。