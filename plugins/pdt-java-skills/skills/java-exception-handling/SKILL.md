---
name: java-exception-handling
description: 當需要設計 Java 應用的例外架構、定義 ErrorCode、撰寫 Spring 全域例外處理器（@RestControllerAdvice），或將領域例外對應到 HTTP 回應時使用。涵蓋 DomainException / BusinessException 的階層設計、ErrorCode enum 規範、GlobalExceptionHandler 實作。
allowed-tools: Read, Write, Glob, Grep
---

# Java Exception Handling Skill

負責例外架構設計與全域處理器實作，確保業務錯誤、驗證錯誤、系統錯誤都有一致的回應格式。

---

## 例外階層

```
RuntimeException
└── DomainException (abstract)         # 領域層基底，含 errorCode
    └── BusinessException              # 應用層業務例外
```

**設計原則**：
- 全部用 `RuntimeException` 體系，不強迫上層 `throws`
- `DomainException` 抽象類別放 Domain 層（純 Java，不依賴 Spring）
- `BusinessException` 放 Application 層
- 所有例外都帶 `errorCode`，方便前端對應錯誤訊息與 i18n

---

## DomainException（Domain 層）

```java
// domain/exception/DomainException.java
public abstract class DomainException extends RuntimeException {
    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

---

## BusinessException 與 ErrorCode（Application 層）

```java
// application/exception/BusinessException.java
public class BusinessException extends DomainException {
    public BusinessException(ErrorCode code) {
        super(code.getCode(), code.getMessage());
    }

    public BusinessException(ErrorCode code, String detail) {
        super(code.getCode(), code.getMessage() + ": " + detail);
    }
}
```

```java
// application/exception/ErrorCode.java
public enum ErrorCode {
    // 訂單
    ORDER_NOT_FOUND("error.order.not_found", "找不到訂單"),
    ORDER_CANNOT_BE_CONFIRMED("error.order.cannot_confirm", "訂單無法確認"),
    ORDER_CANNOT_BE_CANCELLED("error.order.cannot_cancel", "訂單無法取消"),
    INVALID_QUANTITY("error.order.invalid_quantity", "數量無效"),

    // 客戶
    CUSTOMER_NOT_FOUND("error.customer.not_found", "找不到客戶"),

    // 共用
    CURRENCY_MISMATCH("error.common.currency_mismatch", "幣別不一致");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
```

### ErrorCode 命名規則

格式：`error.<模組>.<情境>`

| 範例 | 說明 |
| --- | --- |
| `error.order.not_found` | 訂單不存在 |
| `error.order.cannot_confirm` | 訂單狀態不允許確認 |
| `error.customer.duplicate_email` | 客戶 email 已存在 |
| `error.auth.invalid_token` | 認證 token 無效 |

不要用 `ERROR_001` 之類的數字編碼，前端與 i18n 對應困難。

---

## ErrorCode 對應 HTTP Status

| ErrorCode 類型 | HTTP Status | 範例 |
| --- | --- | --- |
| `*.not_found` | 404 | `error.order.not_found` |
| `*.duplicate_*`、`*.already_exists` | 409 | `error.customer.duplicate_email` |
| `*.cannot_*`、`*.invalid_*` | 422 | `error.order.cannot_confirm` |
| `*.unauthorized` | 401 | `error.auth.invalid_token` |
| `*.forbidden` | 403 | `error.order.access_denied` |

> Status code 對應規則見 `java-rest-api`。

---

## 在 Domain / Service 拋出例外

**Domain 層**（拋邏輯違反）：

```java
public void confirm() {
    if (this.status != OrderStatus.PENDING) {
        throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CONFIRMED);
    }
    this.status = OrderStatus.CONFIRMED;
}
```

**Application 層**（拋找不到、權限）：

```java
Order order = orderRepository.findById(orderId)
    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
```

---

## 全域例外處理器

放在 `adapter/in/web/`，集中對應例外到 HTTP 回應：

```java
// adapter/in/web/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = mapToHttpStatus(ex.getErrorCode());
        log.warn("業務例外, code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status)
            .body(new ErrorResponse(false, ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(false, "error.validation", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("未預期錯誤", ex);
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse(false, "error.internal", "系統錯誤"));
    }

    private HttpStatus mapToHttpStatus(String errorCode) {
        if (errorCode.endsWith(".not_found")) return HttpStatus.NOT_FOUND;
        if (errorCode.contains(".duplicate_") || errorCode.endsWith(".already_exists"))
            return HttpStatus.CONFLICT;
        if (errorCode.endsWith(".unauthorized")) return HttpStatus.UNAUTHORIZED;
        if (errorCode.endsWith(".forbidden")) return HttpStatus.FORBIDDEN;
        return HttpStatus.UNPROCESSABLE_ENTITY;  // 預設 422
    }
}

public record ErrorResponse(boolean success, String code, String message) {}
```

---

## Logging 等級

| 例外類型 | Log 等級 | 理由 |
| --- | --- | --- |
| `BusinessException` | `WARN` | 業務正常分支，不需告警，但需追蹤 |
| Validation 錯誤 | `INFO` 或不記 | 客戶端錯誤，量大易洗版 |
| 未預期 `Exception` | `ERROR` | 需告警與排查 |

業務例外不要記 `ERROR` 也不要印 stack trace，會造成 log 噪音。

---

## 常見錯誤

| 錯誤 | 正確做法 |
| --- | --- |
| 業務例外用 `throw new RuntimeException("找不到訂單")` | 定義 `ErrorCode` 並拋 `BusinessException` |
| 錯誤訊息直接寫死中文字串散落各處 | 集中在 `ErrorCode` enum |
| 全部錯誤都回 500 | 用 `@RestControllerAdvice` 對應正確 status |
| `BusinessException` 印完整 stack trace | 用 WARN 等級且不印 stack |
| Controller 自己 try-catch 包例外 | 統一交給 `GlobalExceptionHandler` |
| ErrorCode 用 `ORDER_001` 數字編碼 | 用語意化 `error.order.not_found` |
