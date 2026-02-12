---
name: java-developer
description: Java/Spring Boot 開發實作規範。當需要撰寫 Java 程式碼、實作業務邏輯、建立 API、處理資料庫操作時使用。
allowed-tools: Read, Write, Glob, Grep, Bash(mvn:*, gradle:*)
---

# Java 開發工程師 Skill

## 職責

依據架構設計實作業務邏輯、撰寫高品質程式碼、確保程式碼可讀性與可測試性。
且不會過度設計

---

## Domain 層依賴規範

### ✅ 允許的依賴

- Spring Core 註解（`@Service`, `@Component`, `@RequiredArgsConstructor`）
- Lombok 註解（`@Getter`, `@Builder`, `@Value`, `@Slf4j`）
- JDK 標準庫
- Spring Data 介面（`Page`, `Pageable`）

### ❌ 禁止的依賴

- Spring Web（`@Controller`, `@RestController`, `@RequestMapping`）
- JPA 實作（`@Entity`, `@Table`, `@Column`）
- 基礎建設框架（Redis, MQ 等）

---

## Entity 實作

```java
// domain/model/Order.java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order {
    private Long id;
    private String orderNumber;
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // 工廠方法
    public static Order create(CustomerId customerId, List<OrderItem> items) {
        return Order.builder()
            .orderNumber(generateOrderNumber())
            .customerId(customerId)
            .items(new ArrayList<>(items))
            .totalAmount(calculateTotal(items))
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // 領域行為 - 業務邏輯放這裡
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CONFIRMED);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
    }

    private static Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

---

## Value Object 實作

```java
// domain/valueobject/Money.java
@Value  // Lombok: 不可變物件
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.TWD);

    BigDecimal amount;
    Currency currency;

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new BusinessException(ErrorCode.CURRENCY_MISMATCH);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}

// domain/valueobject/Email.java
public final class Email {
    private static final Pattern PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    private final String value;

    public Email(String value) {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("無效的電子郵件: " + value);
        }
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(value, ((Email) o).value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }
}
```

---

## Repository Interface

```java
// domain/repository/OrderRepository.java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerId(CustomerId customerId);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    void delete(Order order);
}
```

---

## Application Service 實作

```java
// application/service/OrderService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PricingService pricingService;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderCommand command) {
        log.info("建立訂單, customerId={}", command.customerId());

        // 1. 載入依賴
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 2. 建構領域物件
        List<OrderItem> items = command.items().stream()
            .map(cmd -> OrderItem.create(cmd.productId(), cmd.quantity(), cmd.unitPrice()))
            .toList();

        // 3. 執行領域邏輯
        Order order = Order.create(customer.getId(), items);
        pricingService.applyDiscounts(order, customer);

        // 4. 持久化
        Order saved = orderRepository.save(order);
        log.info("訂單已建立, orderNumber={}", saved.getOrderNumber());

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .map(OrderResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.confirm();  // 呼叫領域行為
        orderRepository.save(order);
    }
}
```

---

## Command / Query DTO

```java
// application/dto/command/CreateOrderCommand.java
public record CreateOrderCommand(
    String customerId,
    List<OrderItemCommand> items,
    String shippingAddress
) {
    public CreateOrderCommand {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_REQUIRED);
        }
    }
}

public record OrderItemCommand(
    String productId,
    int quantity,
    BigDecimal unitPrice
) {
    public OrderItemCommand {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
    }
}

// application/dto/response/OrderResponse.java
public record OrderResponse(
    Long id,
    String orderNumber,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount().getAmount(),
            order.getStatus().name(),
            order.getCreatedAt()
        );
    }
}
```

---

## REST Controller 實作

```java
// adapter/in/web/OrderController.java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "訂單管理")
public class OrderController {

    private final OrderService orderService;

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
    }

    private CreateOrderCommand toCommand(CreateOrderRequest request) {
        return new CreateOrderCommand(
            request.customerId(),
            request.items().stream()
                .map(i -> new OrderItemCommand(i.productId(), i.quantity(), i.unitPrice()))
                .toList(),
            request.shippingAddress()
        );
    }
}
```

---

## Request DTO 與驗證

```java
// adapter/in/web/request/CreateOrderRequest.java
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

---

## Repository 實作

```java
// adapter/out/persistence/JpaOrderRepository.java
@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository jpaRepo;

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.from(order);
        return jpaRepo.save(entity).toDomain();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepo.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return jpaRepo.findByStatus(status, pageable).map(OrderEntity::toDomain);
    }
}

// Spring Data JPA
interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
    
    // ✅ 避免 N+1：使用 @EntityGraph
    @EntityGraph(attributePaths = {"orderItems", "customer"})
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status")
    List<OrderEntity> findByStatusWithDetails(@Param("status") OrderStatus status);
}
```

---

## 例外處理

### 領域例外

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

// application/exception/BusinessException.java
public class BusinessException extends DomainException {
    public BusinessException(ErrorCode code) {
        super(code.getCode(), code.getMessage());
    }
}

// application/exception/ErrorCode.java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    ORDER_NOT_FOUND("error.order.not_found", "找不到訂單"),
    ORDER_CANNOT_BE_CONFIRMED("error.order.cannot_confirm", "訂單無法確認"),
    ORDER_CANNOT_BE_CANCELLED("error.order.cannot_cancel", "訂單無法取消"),
    CUSTOMER_NOT_FOUND("error.customer.not_found", "找不到客戶"),
    INVALID_QUANTITY("error.order.invalid_quantity", "數量無效");

    private final String code;
    private final String message;
}
```

### 全域例外處理器

```java
// adapter/in/web/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(false, ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(false, "VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity
            .internalServerError()
            .body(new ErrorResponse(false, "INTERNAL_ERROR", "系統錯誤"));
    }
}

public record ErrorResponse(boolean success, String code, String message) {}
```

---

## 快取使用

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    @Cacheable(value = "orders", key = "#orderId")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .map(OrderResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @CacheEvict(value = "orders", key = "#orderId")
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long orderId, UpdateOrderCommand command) {
        // 更新邏輯
    }
}
```

---

## 配置檔案規範

### application.yml 結構

```yaml
spring:
  application:
    name: order-service
  profiles:
    active: dev

  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_batch_fetch_size: 100

logging:
  level:
    com.company.project: DEBUG

# 自訂配置
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000
  business:
    order:
      max-items: 100
```

### 配置讀取

```java
// 偏好：使用 @ConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "app.business.order")
@Validated
@Getter @Setter
public class OrderProperties {
    @Min(1)
    private int maxItems = 100;
}

// 或使用 @Value
@Service
public class OrderService {
    @Value("${app.business.order.max-items}")
    private int maxOrderItems;
}
```

---

## 程式碼規範

### 命名規範

| 類型 | 規範 | 範例 |
|------|------|------|
| Class | PascalCase | `OrderService` |
| Method/Variable | camelCase | `createOrder` |
| Constant | UPPER_SNAKE | `MAX_ITEMS` |
| Package | lowercase | `com.company.order` |

### Transaction 使用

```java
// ✅ 寫入操作
@Transactional(rollbackFor = Exception.class)
public void updateOrder(...) { }

// ✅ 唯讀查詢
@Transactional(readOnly = true)
public OrderResponse getOrder(...) { }
```

### 避免 N+1 查詢

```java
// ❌ N+1 問題
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems();  // 每次都查詢
}

// ✅ 使用 @EntityGraph 或 JOIN FETCH
@EntityGraph(attributePaths = {"items"})
List<Order> findAllWithItems();
```

### SQL Injection 防護

```java
// ✅ 使用參數化查詢
@Query("SELECT o FROM Order o WHERE o.orderNumber = :num")
Optional<Order> findByOrderNumber(@Param("num") String orderNumber);

// ❌ 避免字串拼接
String sql = "SELECT * FROM orders WHERE num = '" + num + "'";
```

---

## 測試規範

### 單元測試

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock CustomerRepository customerRepository;
    @InjectMocks OrderService orderService;

    @Test
    void createOrder_shouldSuccess() {
        // Given
        when(customerRepository.findById(any()))
            .thenReturn(Optional.of(createCustomer()));
        when(orderRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderResponse response = orderService.createOrder(createCommand());

        // Then
        assertThat(response).isNotNull();
        verify(orderRepository).save(any());
    }
}
```

---

## 常見錯誤

| 錯誤 | 正確做法 |
|------|----------|
| Domain 層依賴 JPA 註解 | Domain 只用純 Java + Lombok |
| Controller 有業務邏輯 | 業務邏輯放 Service |
| Service 直接回傳 Entity | 使用 Response DTO |
| 忘記 @Transactional | 寫入用 rollbackFor，查詢用 readOnly |
| N+1 查詢 | 使用 @EntityGraph 或 JOIN FETCH |
