---
name: java-clean-arch
description: 當需要設計系統架構、技術選型，或撰寫 Domain/Application/Adapter/Infrastructure 各層程式碼時使用。
allowed-tools: Read, Write, Glob, Grep, Bash(mvn:*, gradle:*)
---

# Clean Architecture Java Skill

## 職責

負責 Clean Architecture 系統設計與各層程式碼實作：確保架構分層正確、依賴規則遵守、程式碼高品質且不過度設計。

---

# 第一部分：設計階段

## 架構分層

```
┌─────────────────────────────────────────────────────────┐
│                    Frameworks & Drivers                  │
│  (Web, DB, External APIs, UI)                           │
│                                                          │
│  ┌───────────────────────────────────────────────────┐ │
│  │              Interface Adapters                    │ │
│  │  (Controllers, Presenters, Gateways)              │ │
│  │                                                    │ │
│  │  ┌─────────────────────────────────────────────┐ │ │
│  │  │        Application Business Rules           │ │ │
│  │  │  (Use Cases, Services)                      │ │ │
│  │  │                                              │ │ │
│  │  │  ┌───────────────────────────────────────┐ │ │ │
│  │  │  │   Enterprise Business Rules           │ │ │ │
│  │  │  │  (Entities, Domain Services)          │ │ │ │
│  │  │  └───────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## 依賴規則

**由外向內依賴，禁止反向依賴**：

```
Infrastructure → Adapter → Application → Domain
        ✓           ✓           ✓
Domain ↛ Application ↛ Adapter ↛ Infrastructure
    ✗           ✗           ✗
```

## 標準套件結構

```
com.company.project/
├── domain/                      # 企業業務規則層
│   ├── model/                   # Entities, Value Objects
│   ├── service/                 # Domain Services
│   └── repository/              # Repository Interfaces
│
├── application/                 # 應用業務規則層
│   ├── service/                 # Use Cases
│   ├── dto/                     # Command, Query, Response
│   └── exception/               # Business Exceptions
│
├── adapter/                     # 介面配接器層
│   ├── in/                      # Inbound (web, messaging)
│   └── out/                     # Outbound (persistence, external)
│
└── infrastructure/              # 基礎建設層
    ├── config/                  # Configuration
    ├── security/                # Security
    └── common/                  # Utilities
```

---

## 核心概念識別

| 概念 | 定義 | 範例 |
|------|------|------|
| **Entity** | 具有唯一識別的業務物件 | Order, User, Product |
| **Value Object** | 無唯一識別的不可變物件 | Money, Address, Email |
| **Aggregate** | 一組相關聯的 entities | Order (root) + OrderItems |
| **Domain Service** | 不屬於任何 entity 的業務邏輯 | PricingService |


---

## 技術選型

### 架構模式

| 模式 | 適用場景 | 優缺點 |
|------|---------|--------|
| **Clean Architecture** | 複雜業務、長期維護 | ✅ 高內聚低耦合 ❌ 初期成本高 |
| **Layered Architecture** | 簡單 CRUD | ✅ 開發快速 ❌ 層級耦合強 |
| **Microservices** | 大型分散式系統 | ✅ 獨立部署 ❌ 運維複雜 |

### 資料庫

| 資料庫 | 適用場景 |
|--------|---------|
| **PostgreSQL** | 關聯式資料、複雜查詢、強 ACID |
| **MySQL** | 關聯式資料、讀多寫少 |
| **MongoDB** | 文件型資料、彈性 schema |
| **Redis** | 快取、Session |

**建議**：主資料庫用 PostgreSQL，快取用 Redis

### API 風格

| 風格 | 適用場景 |
|------|---------|
| **REST** | 對外 API、資源導向 CRUD |
| **GraphQL** | 複雜資料查詢需求 |
| **gRPC** | 微服務內部通訊 |

---

## API 設計原則

### RESTful 資源命名

```
✅ 正確
GET    /api/orders           # 查詢列表
GET    /api/orders/{id}      # 查詢單一
POST   /api/orders           # 建立
PUT    /api/orders/{id}      # 更新
DELETE /api/orders/{id}      # 刪除

❌ 錯誤
GET    /api/getOrders        # 不使用動詞
POST   /api/createOrder      # 不使用動詞
```

### 動作命名（非 CRUD）

```
PUT    /api/orders/{id}/confirm    # 確認訂單
PUT    /api/orders/{id}/cancel     # 取消訂單
```

### HTTP Status Code

| Code | 使用時機 |
|------|---------|
| 200 | 成功（GET, PUT, PATCH） |
| 201 | 成功建立（POST） |
| 204 | 成功無內容（DELETE） |
| 400 | 請求格式錯誤 |
| 404 | 資源不存在 |
| 409 | 資源衝突（例：訂單編號重複、帳號已存在） |
| 422 | 業務規則違反（例：訂單狀態不允許此操作） |
| 500 | 伺服器錯誤 |

### 統一回應格式

**成功**（不需要 code）：
```json
{ "success": true, "data": { ... } }
```

**錯誤**（需要 code）：
```json
{ "success": false, "code": "error.order.not_found", "message": "找不到訂單" }
```

---

## 資料庫設計原則

- 符合第三正規化（3NF）
- 主鍵使用 `BIGINT AUTO_INCREMENT`
- 時間戳：`created_at`, `updated_at`
- 軟刪除：`deleted_at`

**建立索引**：WHERE 條件、JOIN、ORDER BY、外鍵欄位

**避免過度索引**：寫入效能下降、儲存空間增加

---

## 模組劃分策略

### 按功能垂直切分

```
com.company.project/
├── order/              # 訂單模組
│   ├── domain/
│   ├── application/
│   └── adapter/
├── customer/           # 客戶模組
├── product/            # 產品模組
└── shared/             # 共用模組
```

### 模組依賴規則

```
order → customer (允許)
customer → order (禁止 - 循環依賴)

解決方案：使用事件或共用模組
order → shared ← customer
```

---

## 設計模式選型

### 決策樹

```
你要解決什麼問題？
├── 需要建立物件（Need to create objects?）
│   └── 需要幾個實例？（How many instances?）
│       ├── 只需一個 ──→ ✅ Singleton
│       └── 多個 ──→ 建構是否複雜？（Complex construction?）
│           ├── 是，參數很多 ──→ ✅ Builder
│           └── 否 ──→ 誰決定具體類別？（Who decides concrete class?）
│               ├── 子類別決定 ──→ ✅ Factory Method
│               ├── 需要產品族 ──→ ✅ Abstract Factory
│               └── 複製既有物件 ──→ ✅ Prototype
│
├── 需要組織物件/類別結構（Need to structure objects/classes?）
│   └── 目標是什麼？（What's the goal?）
│       ├── 匹配不相容介面 ──→ ✅ Adapter
│       ├── 簡化複雜子系統 ──→ ✅ Facade
│       ├── 動態添加職責 ──→ ✅ Decorator
│       ├── 控制物件存取 ──→ ✅ Proxy
│       ├── 樹狀/階層結構 ──→ ✅ Composite
│       ├── 共享以最小化記憶體 ──→ ✅ Flyweight
│       └── 分離抽象與實作 ──→ ✅ Bridge
│
└── 需要管理物件間的行為/通訊（Need to manage behavior/communication?）
    └── 目標是什麼？
        ├── 請求沿鏈傳遞 ──→ ✅ Chain of Responsibility
        ├── 封裝請求/支援撤銷 ──→ ✅ Command
        ├── 定義演算法骨架 ──→ ✅ Template Method
        ├── 可互換的演算法策略 ──→ ✅ Strategy
        ├── 遍歷集合元素 ──→ ✅ Iterator
        ├── 物件間一對多通知 ──→ ✅ Observer
        ├── 物件狀態驅動行為改變 ──→ ✅ State
        ├── 集中管理物件間互動 ──→ ✅ Mediator
        ├── 保存與還原物件狀態 ──→ ✅ Memento
        ├── 對結構新增操作而不修改類別 ──→ ✅ Visitor
        └── 定義語法的解釋器 ──→ ✅ Interpreter
```

### 速查表

#### 創建型模式

| 模式 | 意圖 | 適用場景 |
|------|------|---------|
| **Singleton** | 確保類別只有一個實例 | 全域設定、連線池、日誌管理 |
| **Builder** | 分步驟建構複雜物件 | 參數多、建構過程複雜的物件 |
| **Factory Method** | 讓子類別決定建立哪個具體類別 | 框架中由子類別決定實例化邏輯 |
| **Abstract Factory** | 建立一系列相關物件的產品族 | UI 主題、跨平台元件、資料庫驅動 |
| **Prototype** | 複製既有物件來建立新物件 | 物件建構成本高、需大量相似物件 |

#### 結構型模式

| 模式 | 意圖 | 適用場景 |
|------|------|---------|
| **Adapter** | 轉換不相容介面使其可協作 | 整合第三方 API、舊系統遷移 |
| **Facade** | 為複雜子系統提供簡化介面 | 簡化複雜模組、提供統一入口 |
| **Decorator** | 動態為物件附加額外職責 | 串流包裝、日誌增強、權限疊加 |
| **Proxy** | 提供物件的替代品以控制存取 | 延遲載入、存取控制、快取代理 |
| **Composite** | 以樹狀結構組合物件 | 檔案系統、組織架構、選單層級 |
| **Flyweight** | 共享細粒度物件以減少記憶體 | 大量相似物件（文字字元、地圖圖塊） |
| **Bridge** | 將抽象與實作分離使其獨立變化 | 跨平台繪圖、裝置驅動程式 |

#### 行為型模式

| 模式 | 意圖 | 適用場景 |
|------|------|---------|
| **Chain of Responsibility** | 請求沿處理者鏈傳遞 | 日誌處理鏈、HTTP 中介層、審核流程 |
| **Command** | 將請求封裝為物件 | 操作佇列、撤銷/重做、巨集錄製 |
| **Template Method** | 定義演算法骨架，子類別覆寫步驟 | 框架 hook 方法、ETL 流程 |
| **Strategy** | 定義可互換的演算法族 | 排序策略、定價規則、驗證邏輯 |
| **Iterator** | 依序存取集合元素而不暴露內部結構 | 自訂集合遍歷 |
| **Observer** | 一對多依賴，狀態改變自動通知 | 事件系統、訊息訂閱、UI 資料綁定 |
| **State** | 物件行為隨內部狀態改變 | 訂單狀態機、工作流程引擎 |
| **Mediator** | 集中管理物件間的互動 | 聊天室、表單元件聯動、航空管制 |
| **Memento** | 保存並還原物件先前狀態 | 撤銷功能、快照、版本管理 |
| **Visitor** | 在不修改類別的情況下新增操作 | 編譯器 AST 遍歷、報表生成 |
| **Interpreter** | 為語言定義文法與解譯器 | DSL 解析、規則引擎、查詢語言 |

### 選型原則

1. **優先使用組合而非繼承** — Decorator、Strategy、Composite 優先
2. **針對介面編程** — 依賴抽象而非具體實作
3. **單一職責** — 一個模式解決一個問題，不要過度設計
4. **YAGNI** — 確實需要時才引入模式，避免預設過度抽象

---

## 效能與擴展性設計

- **快取**：`Client → HTTP Cache → Redis → Database`
- **水平擴展**：無狀態設計，Session 存 Redis，多實例部署
- **非同步**：非關鍵路徑操作改用訊息佇列（RabbitMQ, Kafka）

---

## 架構決策記錄（ADR）

```markdown
# ADR-NNN: <決策標題>

## 狀態：<提議 / 已接受 / 已棄用>

## 背景
<為何需要這個決策？面臨什麼問題？>

## 決策
<採用什麼方案？>

## 理由
<為何選這個方案而非其他？>

## 後果
- 優點：
- 缺點：
```

---

## 架構審查檢查清單

### 整體架構
- [ ] 符合 Clean Architecture 分層
- [ ] 依賴規則正確（由外向內）
- [ ] 模組職責清楚單一
- [ ] 無循環依賴

### 技術選型
- [ ] 技術選擇有明確理由
- [ ] 考慮長期維護成本
- [ ] 團隊熟悉度

### 設計模式
- [ ] 選用模式有對應的問題場景
- [ ] 未過度設計（YAGNI）
- [ ] 模式使用符合依賴規則

### 效能與安全
- [ ] 快取策略合理
- [ ] 認證授權機制完善

---

# 第二部分：實作階段

## 各層依賴規範

### Domain 層 — 純 Java，不依賴任何框架

- ✅ JDK 標準庫（含 `record`）
- ❌ Spring 任何模組（`@Service`, `@Component`, `@Entity`, `@Controller` 等）
- ❌ 基礎建設框架（Redis, MQ, JPA 等）

> Domain 是最內層，Entity 和 Value Object 使用純 Java 手寫，不引入框架。

### Application Service 層

- ✅ Spring Core（`@Service`, `@Transactional`）
- ✅ SLF4J（`LoggerFactory`）
- ✅ Spring Data 介面（`Page`, `Pageable`）
- ❌ Spring Web、JPA Entity、基礎建設框架

### Adapter / Infrastructure 層

可依賴所有框架與技術（Spring Web、JPA、Redis 等），但不可反向依賴 Application / Domain。

---

## Entity 實作

```java
// domain/model/Order.java
public class Order {
    private Long id;
    private String orderNumber;
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    protected Order() {}

    private Order(Long id, String orderNumber, CustomerId customerId,
                  List<OrderItem> items, Money totalAmount,
                  OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public CustomerId getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Money getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // 工廠方法
    public static Order create(CustomerId customerId, List<OrderItem> items) {
        return new Order(
            null,
            generateOrderNumber(),
            customerId,
            new ArrayList<>(items),
            calculateTotal(items),
            OrderStatus.PENDING,
            LocalDateTime.now()
        );
    }

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
// domain/valueobject/Money.java — 使用 record 實作不可變物件
public record Money(BigDecimal amount, Currency currency) {
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.TWD);

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
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PricingService pricingService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        PricingService pricingService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.pricingService = pricingService;
    }

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
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository jpaRepo;

    public JpaOrderRepository(SpringDataOrderRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

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
public enum ErrorCode {
    ORDER_NOT_FOUND("error.order.not_found", "找不到訂單"),
    ORDER_CANNOT_BE_CONFIRMED("error.order.cannot_confirm", "訂單無法確認"),
    ORDER_CANNOT_BE_CANCELLED("error.order.cannot_cancel", "訂單無法取消"),
    CUSTOMER_NOT_FOUND("error.customer.not_found", "找不到客戶"),
    INVALID_QUANTITY("error.order.invalid_quantity", "數量無效");

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

在既有的 Service 方法上加上快取註解即可：

```java
// ✅ 查詢加快取
@Cacheable(value = "orders", key = "#orderId")
@Transactional(readOnly = true)
public OrderResponse getOrder(Long orderId) {
    return orderRepository.findById(orderId)
        .map(OrderResponse::from)
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
}

// ✅ 寫入時清除快取
@CacheEvict(value = "orders", key = "#orderId")
@Transactional(rollbackFor = Exception.class)
public void updateOrder(Long orderId, UpdateOrderCommand command) {
    // 更新邏輯
}
```

---

## 配置檔案規範

```yaml
spring:
  application:
    name: order-service
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

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000
  business:
    order:
      max-items: 100
```

```java
// ✅ 偏好：使用 @ConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "app.business.order")
@Validated
public class OrderProperties {
    @Min(1)
    private int maxItems = 100;

    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
}

// ❌ 禁止 @Value field injection（違反建構子注入原則）
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
    order.getItems();  // 每次都觸發額外查詢
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

> 測試撰寫規範（JUnit 5、AssertJ、Mockito、TDD 流程）請參考 `java-tdd` skill。

- Domain 層（Entity、Value Object）使用純單元測試，無需 Mock
- Application Service 測試：Mock Repository 與外部依賴
- Controller 測試：使用 `@WebMvcTest`，Mock Service
- Repository 測試：使用 `@DataJpaTest`，連真實資料庫

---

## 常見錯誤

| 錯誤 | 正確做法 |
|------|----------|
| Domain 層依賴 JPA 註解 | Domain 只用純 Java（record、手寫 getter） |
| Controller 有業務邏輯 | 業務邏輯放 Service |
| Service 直接回傳 Entity | 使用 Response DTO |
| 忘記 @Transactional | 寫入用 rollbackFor，查詢用 readOnly |
| N+1 查詢 | 使用 @EntityGraph 或 JOIN FETCH |

---

## 參考資源

- Clean Architecture (Robert C. Martin)
- Domain-Driven Design (Eric Evans)
- Building Microservices (Sam Newman)
- Design Patterns: Elements of Reusable Object-Oriented Software (GoF)
