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

2. **編譯專案**
   ```bash
   mvn clean compile
   ```

3. **啟動應用**
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
- SQLite 資料庫位於 `./data/library.db`
- 每次啟動會重建資料表（開發模式）

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

## 文件

- [架構設計](ARCHITECTURE.md) - 系統架構、設計決策、技術選型
- [開發規範](CLAUDE.md) - 編碼標準、最佳實踐、開發流程

## 貢獻指南

1. 遵循 [CLAUDE.md](CLAUDE.md) 中的開發規範
2. 撰寫測試優先（TDD）
3. 確保測試覆蓋率 > 80%
4. 所有 PR 必須通過 Code Review

## 授權

此專案為考題實作，僅供學習參考使用。