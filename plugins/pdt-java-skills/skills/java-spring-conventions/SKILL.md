---
name: java-spring-conventions
description: 當需要撰寫 Spring Boot 應用，涉及 @Transactional 設定、避免 JPA N+1 查詢、@ConfigurationProperties 配置、@Cacheable 快取、依賴注入、Java 命名與資料庫設計規範時使用。涵蓋 Spring 開發的橫切慣例與常見效能陷阱。不包含架構分層（見 java-clean-arch）。
allowed-tools: Read, Write, Glob, Grep
---

# Java Spring Conventions Skill

負責 Spring Boot 應用的橫切慣例：交易、ORM 效能、配置、快取、命名規範、SQL 安全。

---

## 依賴注入

**一律建構子注入**，不用 `@Autowired` 欄位注入：

```java
// ✅ 正確
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PricingService pricingService;

    public OrderService(OrderRepository orderRepository, PricingService pricingService) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
    }
}

// ❌ 禁止：欄位注入
@Autowired private OrderRepository orderRepository;

// ❌ 禁止：Setter 注入（除非框架特殊需求）
```

理由：建構子注入支援 `final`、利於測試、依賴在編譯期就確定。

---

## Transaction

```java
// ✅ 寫入操作：明確指定 rollbackFor，避免只 rollback unchecked exception
@Transactional(rollbackFor = Exception.class)
public OrderResponse createOrder(CreateOrderCommand command) {
    // ...
}

// ✅ 唯讀查詢：readOnly 提示 ORM 與 DB 最佳化
@Transactional(readOnly = true)
public OrderResponse getOrder(Long id) {
    // ...
}
```

**規範重點**：
- `@Transactional` 標在 **Application Service 層**，不要標在 Controller 或 Domain
- 寫入一律 `rollbackFor = Exception.class`（預設只 rollback `RuntimeException`，checked exception 不會 rollback）
- 唯讀加 `readOnly = true`
- 同一 class 內方法互相呼叫，內層 `@Transactional` 不會生效（用 self-injection 或拆分）

---

## 避免 N+1 查詢

**N+1 範例**：

```java
// ❌ N+1：載入 N 個 Order，每個再查 items（共 N+1 次查詢）
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems().size();  // 觸發 lazy load
}
```

**解法 1：`@EntityGraph`**

```java
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = {"items", "customer"})
    List<OrderEntity> findByStatus(OrderStatus status);
}
```

**解法 2：JPQL `JOIN FETCH`**

```java
@Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.status = :status")
List<OrderEntity> findByStatusWithItems(@Param("status") OrderStatus status);
```

**解法 3：批次抓取**（多個集合避免笛卡兒積）

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

> 多個 `@OneToMany` 集合同時 `JOIN FETCH` 會產生笛卡兒積，請改用 `@EntityGraph` + `default_batch_fetch_size`。

---

## SQL Injection 防護

```java
// ✅ 參數化查詢（JPQL / Native Query 都適用）
@Query("SELECT o FROM Order o WHERE o.orderNumber = :num")
Optional<Order> findByOrderNumber(@Param("num") String orderNumber);

// ❌ 字串拼接：高危
String sql = "SELECT * FROM orders WHERE num = '" + num + "'";
entityManager.createNativeQuery(sql);
```

動態查詢條件用 `Specification` 或 QueryDSL，**永遠不要字串拼接 SQL**。

---

## 配置：偏好 @ConfigurationProperties

```yaml
# application.yml
app:
  business:
    order:
      max-items: 100
      max-amount: 1000000
```

```java
// ✅ 結構化配置 + 驗證
@Configuration
@ConfigurationProperties(prefix = "app.business.order")
@Validated
public class OrderProperties {
    @Min(1)
    private int maxItems = 100;

    @Min(0)
    private long maxAmount = 1_000_000;

    // getters / setters
}

// ❌ 禁止：@Value field injection
@Value("${app.business.order.max-items}")
private int maxItems;
```

理由：`@Value` 散落各處難維護、無法驗證、難以測試。

### 配置標準結構

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
      ddl-auto: validate    # 正式環境一律 validate
    properties:
      hibernate:
        default_batch_fetch_size: 100

app:
  jwt:
    secret: ${JWT_SECRET}        # 機密用環境變數注入
    expiration: 86400000
```

機密（密碼、token、API key）一律用環境變數，不寫死在 yml。

---

## 快取

在既有方法上加註解：

```java
// ✅ 查詢加快取
@Cacheable(value = "orders", key = "#orderId")
@Transactional(readOnly = true)
public OrderResponse getOrder(Long orderId) { ... }

// ✅ 寫入時清除快取
@CacheEvict(value = "orders", key = "#orderId")
@Transactional(rollbackFor = Exception.class)
public void updateOrder(Long orderId, UpdateOrderCommand command) { ... }

// ✅ 多重清除
@Caching(evict = {
    @CacheEvict(value = "orders", key = "#orderId"),
    @CacheEvict(value = "orderList", allEntries = true)
})
public void cancelOrder(Long orderId) { ... }
```

**注意**：
- `@Cacheable` 與 `@Transactional` 同時用時，順序由 AOP proxy 決定（一般 cache 在外，先查 cache 再進 transaction）
- 同 class 內方法互相呼叫，註解不會生效

---

## 命名規範

| 類型 | 規範 | 範例 |
| --- | --- | --- |
| Class | PascalCase | `OrderService`, `OrderController` |
| Interface | PascalCase（不加 `I` 前綴） | `OrderRepository` |
| Method / Variable | camelCase | `createOrder`, `totalAmount` |
| Constant | UPPER_SNAKE_CASE | `MAX_ITEMS`, `DEFAULT_TIMEOUT` |
| Package | lowercase，無底線 | `com.company.order` |
| Boolean | `is` / `has` / `can` 前綴 | `isActive`, `hasPermission` |
| Test method | `methodName_condition_expectedBehavior` | `createOrder_whenItemsEmpty_throwsException` |

**避免**：
- 縮寫（`OrdMgr` → `OrderManager`）
- 匈牙利命名（`strName`, `iCount`）
- 無意義名稱（`data`, `info`, `process`）

---

## 資料庫設計

- 符合第三正規化（3NF）
- 主鍵 `BIGINT AUTO_INCREMENT`（或 UUID 視業務需要）
- 標準時間戳：`created_at`, `updated_at`
- 軟刪除：`deleted_at`（NULL 表示未刪除）
- 索引建立時機：WHERE 條件、JOIN 欄位、ORDER BY 欄位、外鍵
- 避免過度索引：寫入效能下降、儲存成本上升

---

## 常見錯誤

| 錯誤 | 正確做法 |
| --- | --- |
| `@Autowired` 欄位注入 | 建構子注入 |
| 忘記 `@Transactional` | 寫入用 `rollbackFor`，查詢用 `readOnly` |
| `@Transactional(rollbackFor = Exception.class)` 漏掉 | 一律明確指定 |
| 同 class 內呼叫 `@Transactional` 方法不生效 | self-injection 或拆分到不同 bean |
| N+1 查詢 | `@EntityGraph` 或 `JOIN FETCH` |
| 多個集合 `JOIN FETCH` 造成笛卡兒積 | `default_batch_fetch_size` + `@EntityGraph` |
| `@Value` field injection 散落 | `@ConfigurationProperties` 集中 |
| 字串拼接 SQL | 參數化查詢、Specification |
| 機密寫在 yml | 用 `${ENV_VAR}` 注入 |
| 介面取名 `IOrderRepository` | 不加 `I` 前綴，實作加後綴或放 impl 套件 |
