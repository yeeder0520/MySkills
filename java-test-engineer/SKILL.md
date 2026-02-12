---
name: java-test-engineer
description: 撰寫 JUnit 5、AssertJ、Mockito 測試，遵循 TDD 開發流程。當需要撰寫單元測試、整合測試、或進行 TDD 開發時使用。適用於所有 Java/Spring Boot 專案。
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(mvn:test*, mvn:verify*)
---

# Java 測試工程師 Skill

## 職責

撰寫高品質測試程式碼，確保系統穩定性，遵循 TDD 流程。

---

## 核心原則

### TDD 循環

```
1. Red    → 寫失敗的測試
2. Green  → 寫最少程式碼讓測試通過
3. Refactor → 重構，保持測試通過
```

### 測試金字塔

```
      /\        E2E        少量、慢
     /--\       整合測試    適量
    /----\      單元測試    大量、快
```

---

## 測試框架

| 框架 | 用途 |
|------|------|
| JUnit 5 | 測試框架 |
| AssertJ | 流暢斷言 |
| Mockito | Mock 框架 |
| Spring Boot Test | 整合測試 |

---

## 單元測試

### 標準結構

```java
@ExtendWith(MockitoExtension.class)  // ⚠️ 必須加這行
@DisplayName("OrderService 單元測試")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;  // 或 OrderServiceImpl

    @DisplayName("應該成功建立訂單")
    @Test
    void createOrder_Success_WhenCommandIsValid() {
        // Given - 準備測試資料
        CreateOrderCommand command = new CreateOrderCommand("ORD-001", new BigDecimal("1000"));
        Order expectedOrder = Order.builder()
            .id(1L)
            .orderNumber("ORD-001")
            .status(OrderStatus.PENDING)
            .build();

        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);

        // When - 執行測試
        OrderResponse response = orderService.createOrder(command);

        // Then - 驗證結果
        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo("ORD-001");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);

        verify(orderRepository).save(any(Order.class));
    }

    @DisplayName("應該拋出例外當訂單不存在時")
    @Test
    void getOrder_ThrowsException_WhenNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrder(999L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }
}
```

### 命名規範

| 類型 | 規範 | 範例 |
|------|------|------|
| 測試類別 | `{ClassName}Test` | `OrderServiceTest` |
| 測試方法 | `method_Expected_When` | `createOrder_Success_WhenValid` |

---

## AssertJ 斷言

### 基本斷言

```java
// 物件
assertThat(order).isNotNull();
assertThat(order.getId()).isEqualTo(1L);

// 字串
assertThat(orderNumber).isNotBlank().startsWith("ORD-");

// 數值
assertThat(amount).isEqualByComparingTo("1000.00").isGreaterThan(BigDecimal.ZERO);

// 集合
assertThat(orders).hasSize(3).extracting(Order::getStatus).containsOnly(OrderStatus.PENDING);
```

### 例外斷言

```java
// 驗證拋出例外
assertThatThrownBy(() -> orderService.getOrder(999L))
    .isInstanceOf(BusinessException.class)
    .hasMessage("找不到訂單");

// 驗證不拋例外
assertThatCode(() -> orderService.createOrder(validCommand))
    .doesNotThrowAnyException();
```

---

## Mockito 使用

### Mock 建立

```java
// ✅ 正確：使用 @ExtendWith
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;
}

// ❌ 錯誤：手動初始化（不需要）
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);  // 不要這樣寫
}
```

### Stubbing

```java
// 回傳值
when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

// 拋出例外
when(orderRepository.findById(999L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

// 動態回傳
when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
```

### Verification

```java
// 驗證呼叫
verify(orderRepository).save(any(Order.class));
verify(orderRepository, times(1)).findById(1L);
verify(orderRepository, never()).deleteById(anyLong());

// 驗證參數
verify(orderRepository).save(argThat(order ->
    order.getOrderNumber().equals("ORD-001")
));
```

### ArgumentCaptor

```java
@Test
void createOrder_SavesCorrectOrder() {
    ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
    
    orderService.createOrder(command);
    
    verify(orderRepository).save(captor.capture());
    
    Order captured = captor.getValue();
    assertThat(captured.getOrderNumber()).isEqualTo("ORD-001");
}
```

---

## 整合測試

### Service 整合測試

```java
@SpringBootTest
@Transactional
@DisplayName("OrderService 整合測試")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("應該成功建立並儲存訂單")
    void createOrder_SavesToDatabase() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand("ORD-001", new BigDecimal("1000"));

        // When
        OrderResponse response = orderService.createOrder(command);

        // Then
        assertThat(response.id()).isNotNull();
        
        Order saved = orderRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-001");
    }
}
```

### Controller 測試

```java
@WebMvcTest(OrderController.class)
@DisplayName("OrderController API 測試")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/orders - 應該回傳 201")
    void createOrder_Returns201() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("ORD-001", new BigDecimal("1000"));
        OrderResponse response = new OrderResponse(1L, "ORD-001", OrderStatus.PENDING);

        when(orderService.createOrder(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - 應該回傳 404")
    void getOrder_Returns404_WhenNotFound() throws Exception {
        when(orderService.getOrder(999L))
            .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/orders/{id}", 999L))
            .andExpect(status().isNotFound());
    }
}
```

### Repository 測試

```java
@DataJpaTest
@DisplayName("OrderRepository 測試")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("應該依狀態查詢訂單")
    void findByStatus_ReturnsMatchingOrders() {
        // Given
        orderRepository.save(Order.builder().orderNumber("ORD-001").status(OrderStatus.PENDING).build());
        orderRepository.save(Order.builder().orderNumber("ORD-002").status(OrderStatus.CONFIRMED).build());

        // When
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING);

        // Then
        assertThat(pending).hasSize(1).extracting(Order::getOrderNumber).containsExactly("ORD-001");
    }
}
```

---

## Mock 策略

| ✅ 應該 Mock | ❌ 不應該 Mock |
|-------------|---------------|
| Repository | Domain Entity |
| 外部 API | Value Object |
| 第三方服務 | 被測試的類別本身 |

---

## 測試資料建立

```java
// 使用 Builder
Order order = Order.builder()
    .id(1L)
    .orderNumber("ORD-001")
    .status(OrderStatus.PENDING)
    .build();

// 使用 Test Fixture 方法
private Order createPendingOrder() {
    return Order.builder()
        .orderNumber("ORD-001")
        .status(OrderStatus.PENDING)
        .build();
}
```

---

## 執行測試

```bash
# 執行所有測試
./mvnw test

# 執行特定類別
./mvnw test -Dtest=OrderServiceTest

# 執行特定方法
./mvnw test -Dtest=OrderServiceTest#createOrder_Success_WhenValid

# 產生覆蓋率報告
./mvnw clean test jacoco:report
```

---

## 覆蓋率目標

| 指標 | 目標 |
|------|------|
| 單元測試覆蓋率 | ≥ 80% |
| 分支覆蓋率 | ≥ 70% |
| 重點覆蓋 | Domain / Application 層 |

---

## 檢查清單

- [ ] 使用 `@ExtendWith(MockitoExtension.class)`
- [ ] 使用中文 `@DisplayName`
- [ ] 遵循 Given-When-Then 結構
- [ ] 使用 AssertJ（不用 assertEquals）
- [ ] 只 Mock 外部依賴
- [ ] 測試獨立可重複執行
- [ ] 覆蓋正常與例外情況

---

## 常見錯誤

| ❌ 錯誤 | ✅ 正確 |
|--------|--------|
| `assertEquals("ORD", num)` | `assertThat(num).isEqualTo("ORD")` |
| `@Mock Order order` | `Order order = Order.builder()...` |
| 測試間共用狀態 | 每個測試獨立準備資料 |
| 沒有 `@DisplayName` | 加上中文描述 |
| 手動 `MockitoAnnotations.openMocks()` | 使用 `@ExtendWith` |