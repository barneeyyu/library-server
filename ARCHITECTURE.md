# 系統架構設計

## 概述

線上圖書借閱系統採用分層架構設計，遵循 Domain-Driven Design (DDD) 原則，確保系統的可擴展性、可維護性和可測試性。

## 技術選型

### 核心框架
- **Java 17**: LTS 版本，提供現代語言特性
- **Spring Boot 3.1.4**: 簡化配置，快速開發
- **Spring Data JPA**: 資料持久化抽象層
- **Spring Security**: 認證與授權
- **Lombok**: 減少樣板程式碼

### 資料庫
- **H2**: 輕量級嵌入式資料庫，Spring Boot 原生支援
- **H2 Console**: 內建網頁管理界面

### 測試框架
- **JUnit 5**: 單元測試框架
- **Mockito**: Mock 框架
- **Spring Boot Test**: 整合測試

## 系統架構

### 分層架構

```
┌─────────────────────┐
│   Presentation      │  Controller Layer (REST API)
├─────────────────────┤
│   Application       │  Service Layer (Business Logic)
├─────────────────────┤
│   Domain            │  Entity Layer (Domain Models)
├─────────────────────┤
│   Infrastructure    │  Repository Layer (Data Access)
└─────────────────────┘
```

### 目錄結構

```
src/main/java/com/library/
├── controller/         # REST API 控制器
├── service/           # 業務邏輯服務
├── entity/            # JPA 實體
├── repository/        # 資料存取層
├── config/            # 系統配置
├── dto/               # 資料傳輸物件
├── exception/         # 自定義例外
└── util/              # 工具類別
```

## 核心領域模型

### 實體關係圖

```mermaid
erDiagram
    User ||--o{ BorrowRecord : borrows
    BookCopy ||--o{ BorrowRecord : "is borrowed"
    Book ||--o{ BookCopy : "has copies in"
    Library ||--o{ BookCopy : "stores"
    
    User {
        Long id PK
        String username UK
        String email UK
        String password
        UserRole role
        Boolean active
    }
    
    Library {
        Long id PK
        String name
        String address
        String phone
        Boolean active
    }
    
    Book {
        Long id PK
        String title
        String author
        Integer publishYear
        BookType type
        String isbn
    }
    
    BookCopy {
        Long id PK
        Long bookId FK
        Long libraryId FK
        Integer totalCopies
        Integer availableCopies
        CopyStatus status
    }
    
    BorrowRecord {
        Long id PK
        Long userId FK
        Long bookCopyId FK
        Date borrowDate
        Date dueDate
        Date returnDate
        BorrowStatus status
    }
```

### 業務規則

1. **用戶權限**
   - `MEMBER`: 可借閱、查詢書籍
   - `LIBRARIAN`: 可管理書籍、查看所有借閱記錄

2. **借閱限制**
   - 圖書：每人最多 5 本
   - 書籍：每人最多 10 本
   - 借閱期限：1 個月

3. **庫存管理**
   - 每個圖書館分別管理自己的館藏
   - 同一本書可在多個圖書館有不同數量的副本

## 設計決策

### 1. BookCopy 實體設計

**決策**: 分離 `Book` 和 `BookCopy`

**理由**:
- 遵循正規化原則，避免資料重複
- 支援多館系統，每館獨立管理庫存
- 便於擴展（如書籍狀態管理、館際調撥等）

**替代方案**: 在 Book 中直接存放館別和數量
- **缺點**: 資料重複、搜尋複雜、維護困難

### 2. H2 vs SQLite

**決策**: 選擇 H2

**理由**:
- Spring Boot 原生支援，配置簡單
- 完整的 SQL 標準支援
- 優秀的併發處理能力
- 內建 H2 Console 管理界面
- 支援 HikariCP 連線池
- 適合 Web 應用開發

**SQLite 的問題**:
- 連線池相容性差
- 併發寫入限制
- Spring Boot 整合複雜

### 3. 認證機制

**決策**: JWT + BCrypt

**理由**:
- 無狀態認證，適合 REST API
- BCrypt 安全性高，適合密碼加密
- JWT 便於前後端分離

## 安全設計

### 認證流程

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Auth Controller
    participant S as User Service
    participant D as Database
    
    C->>A: POST /api/auth/login
    A->>S: authenticate(credentials)
    S->>D: findByUsername()
    D->>S: User entity
    S->>S: verify password with BCrypt
    S->>A: JWT token
    A->>C: JWT token + user info
```

### 授權策略

- **端點級別**: 使用 Spring Security 註解
- **方法級別**: `@PreAuthorize` 檢查角色權限
- **資料級別**: Service 層檢查資源擁有權

## 效能考量

### 資料庫最佳化

1. **H2 索引策略**
   ```sql
   -- H2 自動為主鍵和唯一約束建立索引
   CREATE INDEX idx_user_username ON users(username);
   CREATE INDEX idx_book_title ON books(title);
   CREATE INDEX idx_borrow_user_status ON borrow_records(user_id, status);
   ```

2. **查詢最佳化**
   - 使用 JPA 投影避免 N+1 查詢
   - 適當使用 `@BatchSize` 註解
   - H2 支援完整 SQL 語法，關鍵查詢可使用原生 SQL

3. **H2 特定配置**
   - 檔案模式：`jdbc:h2:file:./data/library`
   - 記憶體模式：`jdbc:h2:mem:library`（測試用）
   - H2 Console：開發時的管理界面

4. **快取策略**
   - 書籍資訊使用 `@Cacheable`
   - 用戶權限資訊快取

## 測試策略

### 測試金字塔

```
    ┌─────────────┐
    │     E2E     │  少量，覆蓋關鍵流程
    ├─────────────┤
    │ Integration │  中等，測試組件間互動
    ├─────────────┤
    │    Unit     │  大量，測試業務邏輯
    └─────────────┘
```

### 測試分類

1. **單元測試** (80%)
   - Service 層業務邏輯
   - Repository 層資料存取
   - Utility 方法

2. **整合測試** (15%)
   - Controller 層 API 測試
   - 資料庫操作測試

3. **端到端測試** (5%)
   - 完整用戶流程測試

## 部署架構

### 開發環境
```
┌─────────────┐
│ Spring Boot │
│   (8080)    │
├─────────────┤
│ H2 Database │
│ + H2 Console│
│ (./data/)   │
└─────────────┘
```

## 監控與維運

### 應用監控
- Spring Boot Actuator
- Micrometer metrics
- 自定義健康檢查端點

### 日誌策略
- 結構化日誌 (JSON format)
- 分級記錄 (ERROR, WARN, INFO, DEBUG)
- 敏感資訊遮罩

### 備份策略
- H2 資料庫檔案定期備份
- 定期匯出 SQL 腳本
- 災難恢復計畫

## 技術債務

### 當前限制
1. H2 僅適合開發環境
2. 單體架構擴展性限制
3. 缺乏分散式快取

### 改善計畫
1. 生產環境遷移至 PostgreSQL/MySQL
2. 準備微服務拆分方案
3. 引入 Redis 快取層