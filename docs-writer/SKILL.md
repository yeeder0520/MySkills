---
name: docs-writer
description: 當需要撰寫專案文件時使用：README、API 文件、Javadoc、ADR、CHANGELOG。
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(mvn:javadoc*)
---

# Java 專案文件撰寫

## 文件類型與規範

### README.md

```markdown
# 專案名稱

## 簡介
一句話說明這個服務做什麼。

## 技術棧
- Java 21 / Spring Boot 3.x
- 資料庫、快取、訊息佇列等

## 快速開始
\`\`\`bash
./mvnw spring-boot:run
\`\`\`

## 環境變數
| 變數 | 說明 | 預設值 |
|------|------|--------|
| DB_URL | 資料庫連線 | - |

## API 文件
啟動後訪問 http://localhost:8080/swagger-ui.html
```

---

### Javadoc

只在非顯而易見的地方撰寫，說明「為什麼」而非「做什麼」：

```java
/**
 * 確認訂單。
 * <p>
 * 僅允許 PENDING 狀態的訂單確認。已確認的訂單不得重複確認，
 * 以避免重複觸發下游庫存扣減流程。
 *
 * @throws BusinessException ORDER_CANNOT_BE_CONFIRMED 當訂單不在 PENDING 狀態
 */
public void confirm() { ... }
```

**不需要 Javadoc 的情況**：getter/setter、自解釋的工廠方法、測試方法。

---

### OpenAPI / Swagger 註解

```java
@Operation(summary = "建立訂單", description = "建立新訂單，初始狀態為 PENDING")
@ApiResponse(responseCode = "201", description = "訂單建立成功")
@ApiResponse(responseCode = "409", description = "訂單編號重複")
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) { ... }
```

---

### 架構決策紀錄（ADR）

存放於 `docs/adr/` 目錄，檔名格式：`NNNN-title.md`

```markdown
# NNNN. 決策標題

日期：YYYY-MM-DD
狀態：提議 / 已接受 / 已廢棄

## 背景
為什麼需要做這個決策？

## 決策
我們選擇了什麼方案？

## 理由
為什麼選這個方案？有哪些取捨？

## 後果
這個決策帶來什麼影響？
```

---

### CHANGELOG

遵循 [Keep a Changelog](https://keepachangelog.com/zh-TW/) 格式：

```markdown
## [Unreleased]

## [1.2.0] - 2026-04-22
### Added
- 新增訂單確認 API

### Fixed
- 修正分頁查詢回傳資料不正確的問題
```

---

## 驗證

```bash
# 確認 Javadoc 無錯誤
./mvnw javadoc:javadoc

# 確認 OpenAPI 文件正常產生
./mvnw spring-boot:run
# 訪問 http://localhost:8080/swagger-ui.html
```
