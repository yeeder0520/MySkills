---
name: java-clean-arch
description: 當需要設計 Java 系統架構分層、決定套件結構，或撰寫 Domain/Application/Adapter/Infrastructure 各層程式碼時使用。涵蓋 Clean Architecture 分層規範與依賴規則，但不包含 REST API 設計、例外處理、Spring 慣例（這三項各自獨立成 skill）。
allowed-tools: Read, Write, Glob, Grep, Bash(mvn:*, gradle:*)
---

# Java Clean Architecture Skill

負責 Clean Architecture 系統設計與分層實作：確保架構分層正確、依賴規則遵守、不過度設計。

> **關聯 skills**
> - REST API 設計 → `java-rest-api`
> - 例外處理架構 → `java-exception-handling`
> - Spring 慣例（Transaction、N+1、命名）→ `java-spring-conventions`
> - 測試規範 → `java-tdd`

---

## 架構分層

由內而外四層，**依賴只能由外向內**：

```
Infrastructure → Adapter → Application → Domain
        ✓           ✓           ✓
Domain ↛ Application ↛ Adapter ↛ Infrastructure
    ✗           ✗           ✗
```

| 層 | 職責 | 範例 |
| --- | --- | --- |
| **Domain** | 企業業務規則 | Entity, Value Object, Domain Service, Repository Interface |
| **Application** | 應用業務規則 / Use Cases | Service, Command/Query DTO, Business Exception |
| **Adapter** | 介面配接器 | Controller (in), Repository 實作 (out), External API Client |
| **Infrastructure** | 基礎建設 | Config, Security, 共用工具 |

---

## 標準套件結構

```
com.company.project/
├── domain/
│   ├── model/                   # Entity（純 Java）
│   ├── valueobject/             # Value Object（record）
│   ├── service/                 # Domain Service
│   └── repository/              # Repository Interface
│
├── application/
│   ├── service/                 # Use Case / Application Service
│   ├── dto/                     # Command, Query, Response
│   └── exception/               # Business Exception
│
├── adapter/
│   ├── in/web/                  # REST Controller, Request DTO
│   └── out/persistence/         # JPA Repository 實作, Entity
│
└── infrastructure/
    ├── config/
    ├── security/
    └── common/
```

按功能模組（order, customer, product）垂直切分時，每個模組內部各自維持上述四層結構。

---

## 各層依賴規範

### Domain 層 — 純 Java，禁止任何框架

✅ JDK 標準庫（含 `record`、`Optional`、Stream API）
❌ Spring 任何模組（`@Service`、`@Component`、`@Entity`、`@Controller`）
❌ JPA、Jackson、Lombok 註解
❌ 基礎建設框架（Redis、MQ）

> Domain 是最內層。Entity 與 Value Object 用純 Java 手寫，不引入框架，確保可獨立測試與長期穩定。

### Application 層 — 只用 Spring Core

✅ Spring Core：`@Service`、`@Transactional`
✅ SLF4J：`LoggerFactory`
✅ Spring Data 介面：`Page`、`Pageable`（介面而非實作）
❌ Spring Web（`@RestController` 屬於 Adapter 層）
❌ JPA Entity / Hibernate 註解
❌ 基礎建設框架直接呼叫（透過 Repository 介面）

### Adapter / Infrastructure 層 — 框架自由使用

可依賴所有框架（Spring Web、JPA、Redis、Kafka 等），但**不得反向依賴 Application / Domain**的具體實作（只能依賴介面）。

---

## 模組依賴規則

```
order → customer    ✓ 允許
customer → order    ✗ 禁止（循環依賴）

解決：抽取共用模組或改用領域事件
order → shared ← customer
order  →  events  →  customer
```

---

## 核心概念

| 概念 | 定義 | 範例 |
| --- | --- | --- |
| **Entity** | 具唯一識別的業務物件 | `Order`, `User`, `Product` |
| **Value Object** | 無唯一識別的不可變物件（用 `record`） | `Money`, `Email`, `Address` |
| **Aggregate** | 一組相關 Entity 的集合，有單一 root | `Order` (root) + `OrderItem` |
| **Domain Service** | 不屬於單一 Entity 的業務邏輯 | `PricingService` |
| **Use Case** | 一個應用情境，對應一個 Application Service 方法 | `createOrder`, `confirmOrder` |

---

## 完整範例

完整 Order 模組的可運行程式碼（Entity / Value Object / Service / Controller / Repository）：

→ `examples/order-module/`

範例涵蓋：
- `Order` Entity 含工廠方法與狀態轉移行為（`confirm()`、`cancel()`）
- `Money` Value Object（用 `record`）
- `OrderRepository` 介面定義在 Domain，實作在 Adapter
- `OrderService` 完整 Use Case 流程（載入依賴 → 領域邏輯 → 持久化）
- `OrderController` REST 端點

---

## 常見錯誤

| 錯誤 | 正確做法 |
| --- | --- |
| Domain 層用 `@Entity` 註解 | Domain 純 Java，JPA Entity 放 `adapter/out/persistence/` |
| Domain Entity 直接被 Controller 回傳 | Application 層轉成 Response DTO |
| Service 直接呼叫 JPA Repository | Service 依賴 Domain 的 Repository 介面，實作放 Adapter |
| Repository Interface 放在 Adapter 層 | 介面放 Domain，實作放 Adapter（依賴反轉） |
| 為了「之後可能會用」先做抽象 | YAGNI，等真的需要時再重構 |

---

## 架構審查檢查清單

- [ ] 符合 Clean Architecture 四層分層
- [ ] 依賴方向由外向內，無反向依賴
- [ ] Domain 層無任何框架註解
- [ ] Application 層僅用 Spring Core，不涉 Web / JPA
- [ ] Repository Interface 在 Domain，實作在 Adapter
- [ ] 無模組間循環依賴
- [ ] 未過度設計（YAGNI）
