# 線上圖書借閱系統

基於 Java + Spring Boot 開發的多館圖書借閱管理系統。

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
   ```bash
   # 複製環境變數範例檔案
   cp .env.example .env
   
   # 編輯 .env 檔案，修改敏感參數（如 JWT_SECRET）
   nano .env
   ```

3. **編譯專案**
   ```bash
   mvn clean compile
   ```

4. **啟動應用**
   ```bash
   mvn spring-boot:run
   ```

4. **測試連線**
   ```bash
   curl http://localhost:8080/api/test/health
   ```

## 使用說明

### 測試端點
- `GET /api/test/health` - 健康檢查
- `GET /api/test/hello` - 簡單測試

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

目標測試覆蓋率：> 80%

## 開發工具設定

### IDE 需求
- 安裝 Lombok 插件
- 啟用 Annotation Processing

### 除錯模式
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## API 測試

### 會員註冊範例
```bash
# 註冊一般會員
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "fullName": "測試用戶",
    "role": "MEMBER"
  }'

# 註冊館員（需要驗證 token）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: todo" \
  -d '{
    "username": "librarian",
    "password": "password123",
    "email": "librarian@example.com",
    "fullName": "圖書館員",
    "role": "LIBRARIAN"
  }'
```

### 會員登入範例
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 使用 JWT Token
```bash
# 從登入回應中取得 token，然後：
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/auth/me
```

## Postman 測試

專案包含完整的 Postman 測試集合：
- 匯入 `postman/Library-Management-System.postman_collection.json`
- 詳細說明請見 `postman/README.md`

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
1. **無法連線 H2 Console**：
   - 確認應用程式正在運行
   - 檢查 URL：http://localhost:8080/h2-console

2. **資料庫鎖定問題**：
   - 確保只有一個應用程式實例在運行
   - 重啟應用程式會重建資料庫

3. **JWT Token 過期**：
   - Token 有效期為 24 小時
   - 重新登入獲取新 Token

## 貢獻指南

1. 遵循 [CLAUDE.md](CLAUDE.md) 中的開發規範
2. 撰寫測試優先（TDD）
3. 確保測試覆蓋率 > 80%
4. 所有 PR 必須通過 Code Review

## 授權

此專案為考題實作，僅供學習參考使用。