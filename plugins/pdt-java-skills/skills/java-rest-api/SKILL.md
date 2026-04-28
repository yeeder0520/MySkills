---
name: java-rest-api
description: 當需要設計 RESTful API 端點、決定 HTTP Status Code、定義回應格式，或撰寫 Spring REST Controller 時使用。涵蓋資源命名、動作命名、狀態碼選用、統一回應結構與 Request 驗證。不包含架構分層（見 java-clean-arch）與例外處理（見 java-exception-handling）。
allowed-tools: Read, Write, Glob, Grep
---

# Java REST API Skill

負責 REST API 設計規範與 Spring Controller 實作慣例。

---

## 資源命名

使用名詞、複數、小寫、kebab-case：

```
✅ 正確
GET    /api/v1/orders           # 查詢列表
GET    /api/v1/orders/{id}      # 查詢單一
POST   /api/v1/orders           # 建立
PUT    /api/v1/orders/{id}      # 更新（整體）
PATCH  /api/v1/orders/{id}      # 更新（部分）
DELETE /api/v1/orders/{id}      # 刪除

❌ 錯誤
GET    /api/getOrders           # 動詞放 URL
POST   /api/createOrder         # 動詞放 URL
GET    /api/Order               # 大寫、單數
```

URL 一律加版本前綴 `/api/v1/`，方便日後不相容變更。

## 動作命名（非 CRUD）

無法用 CRUD 表達的業務動作，使用 `PUT /resource/{id}/action`：

```
PUT  /api/v1/orders/{id}/confirm    # 確認訂單
PUT  /api/v1/orders/{id}/cancel     # 取消訂單
PUT  /api/v1/orders/{id}/ship       # 出貨
POST /api/v1/orders/{id}/refunds    # 建立退款（退款是子資源）
```

動作為冪等用 PUT，動作會建立新資源用 POST。

---

## HTTP Status Code

| Code | 使用時機 |
| --- | --- |
| 200 OK | 成功（GET、PUT、PATCH 有回傳內容） |
| 201 Created | 成功建立（POST），回應應含新資源 URL 或 body |
| 204 No Content | 成功但無回應內容（DELETE、確認類動作） |
| 400 Bad Request | 請求格式錯誤、JSON 解析失敗 |
| 401 Unauthorized | 未認證 |
| 403 Forbidden | 已認證但無權限 |
| 404 Not Found | 資源不存在 |
| 409 Conflict | 資源衝突（訂單編號重複、帳號已存在） |
| 422 Unprocessable Entity | 業務規則違反（訂單狀態不允許此操作） |
| 500 Internal Server Error | 伺服器錯誤 |

**409 vs 422 的區別**：
- 409：資源層次的衝突（重複、版本不一致）
- 422：資料合法但違反業務規則（狀態機禁止、額度不足）

---

## 統一回應格式

**成功回應**直接回傳資料，不包 wrapper：

```json
{
  "id": 123,
  "orderNumber": "ORD-20260428-001",
  "totalAmount": 1500,
  "status": "CONFIRMED"
}
```

列表用 Spring `Page`：

```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

**錯誤回應**統一格式（由 GlobalExceptionHandler 產生，見 `java-exception-handling`）：

```json
{
  "success": false,
  "code": "error.order.not_found",
  "message": "找不到訂單"
}
```

> 過去常見的 `{ "success": true, "data": {...} }` 包裝法已不建議用於成功回應，因為 HTTP Status 已表達成功與否，wrapper 反而增加客戶端解析成本。錯誤回應因為需要附帶 code，才使用 wrapper。

---

## Controller 實作慣例

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "訂單管理")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(toCommand(request));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public Page<OrderResponse> listOrders(
        @RequestParam(required = false) OrderStatus status,
        @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable
    ) {
        return orderService.listOrders(status, pageable);
    }

    @PutMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmOrder(@PathVariable Long id) {
        orderService.confirmOrder(id);
    }

    private CreateOrderCommand toCommand(CreateOrderRequest req) {
        // Request DTO → Application Command 的轉換在 Controller 完成
        return new CreateOrderCommand(...);
    }
}
```

**規範重點**：
- 建構子注入，不用 `@Autowired` 或欄位注入
- `@Valid` + `@RequestBody` 啟用 Bean Validation
- Controller 只做 HTTP ↔ Application 邊界轉換，**不放業務邏輯**
- Request DTO 屬於 Adapter 層，不要直接傳給 Service

---

## Request DTO 與驗證

用 `record` 加 Bean Validation 註解：

```java
public record CreateOrderRequest(
    @NotBlank(message = "客戶 ID 為必填")
    String customerId,

    @NotEmpty(message = "訂單項目為必填")
    @Valid
    List<OrderItemRequest> items,

    @NotBlank(message = "配送地址為必填")
    String shippingAddress
) {}

public record OrderItemRequest(
    @NotBlank String productId,
    @Min(1) int quantity,
    @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {}
```

巢狀 DTO 須在外層欄位加 `@Valid` 才會遞迴驗證。

---

## 分頁與排序

統一用 Spring `Pageable`：

```java
@GetMapping
public Page<OrderResponse> list(
    @PageableDefault(size = 20, sort = "createdAt", direction = DESC)
    Pageable pageable
) { ... }
```

請求格式：`GET /api/v1/orders?page=0&size=20&sort=createdAt,desc`

---

## 常見錯誤

| 錯誤 | 正確做法 |
| --- | --- |
| URL 用動詞 `/getOrders` | 用名詞複數 `/orders` |
| 全部用 200，錯誤也回 200 | 正確使用 4xx / 5xx |
| 成功回應包 `{success, data}` wrapper | 直接回傳資料，由 HTTP Status 表達結果 |
| Controller 寫業務邏輯 | 業務邏輯在 Application Service |
| Request DTO 直接傳給 Service | 在 Controller 轉成 Command |
| 忘記 `@Valid` 導致驗證未執行 | `@RequestBody` 前一律加 `@Valid` |
