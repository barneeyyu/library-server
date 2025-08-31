# Postman 測試腳本

線上圖書借閱系統的 Postman API 測試集合。

## 檔案說明

- `Library-Management-System.postman_collection.json` - 主要測試集合（包含所有環境變數）

## 匯入步驟

### 匯入 Collection
1. 開啟 Postman
2. 點擊 **Import**
3. 選擇 `Library-Management-System.postman_collection.json`
4. 點擊 **Import**

## 測試集合內容

### Auth - 會員管理
1. **註冊一般會員** - 測試 MEMBER 角色註冊
2. **註冊館員（成功）** - 測試 LIBRARIAN 角色註冊（有效 token）
3. **註冊館員（失敗）** - 測試無效 librarian token
4. **註冊失敗 - 重複用戶名** - 測試重複註冊
5. **會員登入** - 測試一般會員登入
6. **館員登入** - 測試館員登入
7. **登入失敗** - 測試錯誤密碼
8. **驗證會員認證狀態** - 測試 JWT 認證
9. **驗證館員認證狀態** - 測試館員 JWT 認證
10. **無 Token 訪問受保護端點** - 測試未授權訪問

### Books - 書籍管理
1. **新增書籍（館員）** - 測試館員新增書籍功能
2. **新增書籍（會員 - 應該失敗）** - 測試會員無法新增書籍
3. **新增相同書籍到不同圖書館** - 測試多館系統支援
4. **新增書籍到不存在的圖書館** - 測試錯誤處理
5. **搜尋書籍（依書名）** - 測試書名搜尋功能
6. **搜尋書籍（依作者）** - 測試作者搜尋功能
7. **搜尋書籍（組合條件）** - 測試多條件組合搜尋
8. **搜尋書籍（無條件 - 應該失敗）** - 測試搜尋驗證
9. **取得書籍詳情** - 測試書籍詳情查詢
10. **取得不存在書籍** - 測試 404 錯誤處理

### System - 系統測試
1. **Health Check** - 系統健康檢查
2. **Hello Test** - 基本連通性測試

## 運行測試

### 前置準備
確保 Spring Boot 應用程式正在運行：
```bash
mvn spring-boot:run
```

### 執行測試

#### 方法一：逐個運行
1. 依序點擊每個請求進行測試
2. 查看 **Test Results** 標籤確認測試結果

#### 方法二：批量運行
1. 右鍵點擊 **Library Management System** collection
2. 選擇 **Run collection**
3. 點擊 **Run Library Management System**
4. 查看測試報告

## Collection 變數

| 變數名稱 | 說明 | 預設值 | 作用範圍 |
|---------|------|--------|----------|
| `base_url` | API 基礎 URL | `http://localhost:8080` | Collection |
| `member_token` | 會員 JWT Token | 自動設定 | Collection |
| `librarian_token` | 館員 JWT Token | 自動設定 | Collection |
| `test_book_id` | 測試書籍 ID | 自動設定 | Collection |

> **注意**: 所有變數都是 Collection 變數，只在此測試集合內有效，不會影響其他 Postman collections。

## 測試數據

### 測試用戶

#### 一般會員
- **用戶名**: `testmember`
- **密碼**: `password123`
- **郵箱**: `member@example.com`
- **角色**: `MEMBER`

#### 館員
- **用戶名**: `testlibrarian`
- **密碼**: `password123`
- **郵箱**: `librarian@example.com`
- **角色**: `LIBRARIAN`
- **驗證 Token**: `todo`（放在 Authorization header 中）

## 預期結果

### 成功場景
- ✅ 會員註冊成功，返回 JWT token
- ✅ 館員註冊成功（使用有效 token）
- ✅ 登入成功，返回用戶資訊和 token
- ✅ 認證狀態檢查成功
- ✅ 館員成功新增書籍到圖書館
- ✅ 相同書籍可新增到不同圖書館
- ✅ 書籍搜尋功能正常（書名、作者、年份）
- ✅ 書籍詳情查詢成功
- ✅ 系統健康檢查正常

### 失敗場景
- ❌ 無效館員 token 註冊失敗 (403 Forbidden)
- ❌ 重複用戶名註冊失敗 (400 Bad Request)
- ❌ 錯誤密碼登入失敗 (401 Unauthorized)
- ❌ 會員無法新增書籍 (403 Forbidden)
- ❌ 無法新增書籍到不存在的圖書館 (400 Bad Request)
- ❌ 無搜尋條件的查詢被拒絕 (400 Bad Request)
- ❌ 不存在書籍返回 404 (404 Not Found)
- ❌ 無 token 訪問受保護端點失敗 (401 Unauthorized)

## 自動化測試腳本

測試腳本包含以下自動驗證：
- HTTP 狀態碼檢查
- 回應 JSON 結構驗證
- 業務邏輯正確性驗證
- JWT Token 自動提取和儲存 (Collection 變數)
- 書籍 ID 自動提取和儲存 (Collection 變數)
- 權限控制驗證
- 搜尋結果驗證
- 館藏資訊驗證
- 錯誤訊息驗證

## 故障排除

### 常見問題

1. **連線失敗**
   - 確認 Spring Boot 應用程式正在運行
   - 檢查 `base_url` 設定是否正確

2. **認證失敗**
   - 確認已先執行註冊或登入請求
   - 檢查 token 是否正確設定在環境變數中

3. **測試失敗**
   - 查看 Console 輸出的詳細錯誤訊息
   - 確認資料庫連線正常

### 重置測試環境
如需重新開始測試：
1. 重啟 Spring Boot 應用程式（會重建資料庫）
2. 清空環境變數中的 token
3. 重新執行註冊和登入測試