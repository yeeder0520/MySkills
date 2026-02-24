---
name: three-tier-java-developer
description: 三層式架構 Java/Spring Boot 開發實作規範。當專案採用三層式架構，需要撰寫 Controller、Service、Repository 程式碼時使用。
allowed-tools: Read, Write, Glob, Grep, Bash(mvn:*, gradle:*)
---

# 三層式 Java 開發工程師 Skill

## 適用場景

| ✅ 適合 | ❌ 不適合（改用 Clean Architecture） |
|---------|--------------------------------------|
| 簡單 CRUD 應用 | 複雜業務邏輯 |
| 小型服務（< 10 Entity） | 大型系統（> 20 Entity） |
| 快速開發 / POC / MVP | 長期維護專案 |
| 標準業務流程 | 領域驅動設計（DDD） |

---

## 架構概覽

```
┌─────────────────────────────────────────┐
│     Controller（展示層）                 │  ← HTTP 請求/回應、驗證
├─────────────────────────────────────────┤
│     Service（業務邏輯層）                │  ← 業務邏輯、交易管理
├─────────────────────────────────────────┤
│     Repository（資料存取層）             │  ← 資料存取
├─────────────────────────────────────────┤
│     Entity（資料實體）                   │  ← JPA 映射
└─────────────────────────────────────────┘

依賴方向：Controller → Service → Repository → Entity
```

**核心規則：**
- Controller 不直接呼叫 Repository
- Service 包含業務邏輯與交易管理
- Repository 不包含業務邏輯
- Entity 可使用 JPA 註解

---

## 套件結構

```
com.company.project/
├── controller/
│   ├── OrderController.java
│   └── dto/
│       ├── request/
│       └── response/
├── service/
│   ├── OrderService.java
│   └── impl/
│       └── OrderServiceImpl.java
├── repository/
│   └── OrderRepository.java
├── entity/
│   └── Order.java
├── exception/
│   ├── BusinessException.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
└── config/
```

---

## Entity

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA 需要無參數建構子
    protected Order() {}

    private Order(String orderNumber, String customerName,
                  BigDecimal totalAmount, OrderStatus status) {
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // 工廠方法取代 Builder
    public static Order create(String orderNumber, String customerName,
                               BigDecimal totalAmount, OrderStatus status) {
        return new Order(orderNumber, customerName, totalAmount, status);
    }

    // Getters
    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters（僅暴露業務上需要變更的欄位）
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

---

## Repository

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 方法命名查詢
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatus(OrderStatus status);
    boolean existsByOrderNumber(String orderNumber);

    // @Query 自訂查詢
    @Query("SELECT o FROM Order o WHERE o.totalAmount >= :min")
    List<Order> findByMinAmount(@Param("min") BigDecimal min);

    // 分頁
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
```

---

## Service

```java
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("建立訂單: {}", request.orderNumber());

        if (orderRepository.existsByOrderNumber(request.orderNumber())) {
            throw new BusinessException(ErrorCode.ORDER_NUMBER_DUPLICATED);
        }

        Order order = Order.create(
            request.orderNumber(),
            request.customerName(),
            request.totalAmount(),
            OrderStatus.PENDING
        );

        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CONFIRMED);
        }

        order.setCustomerName(request.customerName());
        order.setTotalAmount(request.totalAmount());

        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CONFIRMED);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_DELETED);
        }

        orderRepository.delete(order);
    }
}
```

---

## Controller

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PutMapping("/{id}")
    public OrderResponse updateOrder(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrderRequest request
    ) {
        return orderService.updateOrder(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

    @PutMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmOrder(@PathVariable Long id) {
        orderService.confirmOrder(id);
    }
}
```

---

## DTO

### Request

```java
public record CreateOrderRequest(
    @NotBlank(message = "訂單編號為必填")
    String orderNumber,

    @NotBlank(message = "客戶名稱為必填")
    String customerName,

    @NotNull @DecimalMin("0.01")
    BigDecimal totalAmount
) {}

public record UpdateOrderRequest(
    @NotBlank(message = "客戶名稱為必填")
    String customerName,

    @NotNull @DecimalMin("0.01")
    BigDecimal totalAmount
) {}
```

### Response

```java
public record OrderResponse(
    Long id,
    String orderNumber,
    String customerName,
    BigDecimal totalAmount,
    OrderStatus status,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerName(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
```

---

## 例外處理

```java
// ErrorCode.java
public enum ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "找不到訂單"),
    ORDER_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "訂單編號已存在"),
    ORDER_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "訂單已確認"),
    ORDER_CANNOT_BE_CONFIRMED(HttpStatus.BAD_REQUEST, "訂單狀態不允許確認"),
    ORDER_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "訂單狀態不允許刪除");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}

// BusinessException.java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}

// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("BusinessException: {}", ex.getMessage());
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getStatus())
            .body(new ErrorResponse(false, code.name(), code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(false, "VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse(false, "INTERNAL_ERROR", "系統錯誤"));
    }
}

public record ErrorResponse(boolean success, String code, String message) {}
```

---

## 規範速查

### Transaction

| 操作 | 註解 |
|------|------|
| 查詢 | `@Transactional(readOnly = true)` |
| 寫入 | `@Transactional(rollbackFor = Exception.class)` |

### 命名

| 類型 | 規則 | 範例 |
|------|------|------|
| Request | `XXXRequest` | `CreateOrderRequest` |
| Response | `XXXResponse` | `OrderResponse` |
| Service | `XXXService` / `XXXServiceImpl` | `OrderService` |

### Logging

```java
// ✅ 正確
private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

log.info("建立訂單: {}", orderNumber);
log.error("建立失敗", exception);

// ❌ 錯誤
System.out.println("...");
e.printStackTrace();
```

---

## 常見錯誤

| 錯誤 | 正確做法 |
|------|----------|
| Entity 用 `@Data` | 手寫 getter/setter，僅暴露需要變更的欄位 |
| Controller 有業務邏輯 | 業務邏輯放 Service |
| Service 回傳 Entity | 回傳 Response DTO |
| 忘記 `@Transactional` | 查詢用 readOnly，寫入用 rollbackFor |
| Repository 有業務判斷 | 業務邏輯放 Service |
| Controller 直接呼叫 Repository | 必須透過 Service |
