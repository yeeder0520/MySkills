# Order Module 範例

完整 Clean Architecture 四層分層示範，可作為新模組起手式。

## 結構

```
order/
├── domain/                              # 純 Java，無框架依賴
│   ├── model/
│   │   ├── Order.java                   # Aggregate Root，封裝狀態轉移
│   │   ├── OrderItem.java               # 屬於 Order 的 Entity
│   │   └── OrderStatus.java
│   ├── valueobject/                     # record 實作不可變值物件
│   │   ├── Money.java
│   │   ├── CustomerId.java
│   │   └── ProductId.java
│   └── repository/
│       └── OrderRepository.java         # 介面，實作放 adapter
│
├── application/                         # 只用 Spring Core
│   ├── service/
│   │   └── OrderService.java            # Use Cases
│   └── dto/
│       ├── CreateOrderCommand.java
│       ├── OrderItemCommand.java
│       └── OrderResponse.java
│
└── adapter/
    ├── in/web/                          # REST 入站
    │   ├── OrderController.java
    │   ├── CreateOrderRequest.java
    │   └── OrderItemRequest.java
    └── out/persistence/                 # JPA 出站
        ├── JpaOrderRepository.java      # 實作 OrderRepository 介面
        ├── SpringDataOrderRepository.java
        └── OrderEntity.java             # JPA Entity，獨立於 Domain
```

## 關鍵設計點

1. **Domain 純 Java**：`Order` 與 `OrderItem` 不帶任何 JPA / Spring 註解
2. **Value Object 用 record**：`Money` 自動取得不可變、equals、hashCode
3. **Repository 介面在 Domain，實作在 Adapter**：依賴反轉
4. **Domain Entity 與 JPA Entity 分離**：`Order` ≠ `OrderEntity`，透過 `from()` / `toDomain()` 轉換
5. **Request DTO 與 Command 分離**：Controller 把 `CreateOrderRequest` 轉成 `CreateOrderCommand` 才呼叫 Service
6. **業務邏輯封裝在 Domain**：`order.confirm()` 內部檢查狀態，Service 不寫 if-else

## 不包含

- 例外處理：見 `java-exception-handling` skill
- REST 慣例細節：見 `java-rest-api` skill
- Transaction、N+1 細節：見 `java-spring-conventions` skill
- 測試：見 `java-tdd` skill
