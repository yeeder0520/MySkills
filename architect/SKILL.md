---
name: architect
description: 負責系統架構設計、技術選型、模組劃分。當需要設計系統架構、進行技術決策、規劃模組結構、或評估架構方案時使用。
allowed-tools: Read, Write, Glob, Grep, Bash(mvn:*)
---

# 架構師 Skill

## 職責

負責系統整體架構設計、技術選型、模組劃分、確保系統可擴展性、可維護性與效能。

---

## 核心架構原則

### Clean Architecture / Hexagonal Architecture

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
│  │  │  └────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 依賴規則

**由外向內依賴，禁止反向依賴**：

```
Infrastructure → Adapter → Application → Domain
        ✓           ✓           ✓
Domain ↛ Application ↛ Adapter ↛ Infrastructure
    ✗           ✗           ✗
```

---

## 設計模式選型決策樹

> 根據你要解決的問題，沿著決策路徑選擇合適的設計模式。

### 起點：What problem are you solving?

```
What problem are you solving?
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

### 設計模式分類速查表

#### 🟢 創建型模式（Creational）

| 模式 | 意圖 | 適用場景 |
|------|------|---------|
| **Singleton** | 確保類別只有一個實例 | 全域設定、連線池、日誌管理 |
| **Builder** | 分步驟建構複雜物件 | 參數多、建構過程複雜的物件 |
| **Factory Method** | 讓子類別決定建立哪個具體類別 | 框架中由子類別決定實例化邏輯 |
| **Abstract Factory** | 建立一系列相關物件的產品族 | UI 主題、跨平台元件、資料庫驅動 |
| **Prototype** | 複製既有物件來建立新物件 | 物件建構成本高、需大量相似物件 |

#### 🔵 結構型模式（Structural）

| 模式 | 意圖 | 適用場景 |
|------|------|---------|
| **Adapter** | 轉換不相容介面使其可協作 | 整合第三方 API、舊系統遷移 |
| **Facade** | 為複雜子系統提供簡化介面 | 簡化複雜模組、提供統一入口 |
| **Decorator** | 動態為物件附加額外職責 | 串流包裝、日誌增強、權限疊加 |
| **Proxy** | 提供物件的替代品以控制存取 | 延遲載入、存取控制、快取代理 |
| **Composite** | 以樹狀結構組合物件 | 檔案系統、組織架構、選單層級 |
| **Flyweight** | 共享細粒度物件以減少記憶體 | 大量相似物件（文字字元、地圖圖塊） |
| **Bridge** | 將抽象與實作分離使其獨立變化 | 跨平台繪圖、裝置驅動程式 |

#### 🔴 行為型模式（Behavioral）

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

### 設計模式選型原則

1. **優先使用組合而非繼承** — Decorator、Strategy、Composite 優先
2. **針對介面編程** — 依賴抽象而非具體實作
3. **單一職責** — 一個模式解決一個問題，不要過度設計
4. **YAGNI** — 確實需要時才引入模式，避免預設過度抽象

---

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

## 架構設計流程

### 1. 需求分析 - 識別核心概念

| 概念 | 定義 | 範例 |
|------|------|------|
| **Entity** | 具有唯一識別的業務物件 | Order, User, Product |
| **Value Object** | 無唯一識別的不可變物件 | Money, Address, Email |
| **Aggregate** | 一組相關聯的 entities | Order (root) + OrderItems |
| **Domain Service** | 不屬於任何 entity 的業務邏輯 | PricingService |

### 2. 分析範例

```markdown
## 訂單管理系統

### 核心概念
- Entity: Order, OrderItem, Customer
- Value Object: Money, Address, OrderStatus
- Aggregate: Order (root) + OrderItems
- Domain Service: PricingService, DiscountService

### Use Cases
1. 建立訂單  2. 更新訂單  3. 確認訂單  4. 取消訂單  5. 查詢訂單

### 對外介面
- REST API → 資料庫 → 付款服務
```

---

## 技術選型決策

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
| 409 | 資源衝突 |
| 422 | 業務規則違反 |
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

### 表格設計

- 符合第三正規化（3NF）
- 主鍵使用 `BIGINT AUTO_INCREMENT`
- 時間戳：`created_at`, `updated_at`
- 軟刪除：`deleted_at`

### 索引策略

**建立索引**：WHERE 條件、JOIN、ORDER BY、外鍵欄位

**避免過度索引**：寫入效能下降、儲存空間增加

---

## 效能與擴展性設計

### 快取策略

```
Client → HTTP Cache → Application Cache (Redis) → Database
```

### 水平擴展

- **無狀態設計**：Session 存 Redis
- **負載均衡**：多實例部署

### 非同步處理

- 使用訊息佇列（RabbitMQ, Kafka）
- 非關鍵路徑操作非同步化

---

## 架構決策記錄（ADR）

```markdown
# ADR-001: 採用 Clean Architecture

## 狀態：已接受

## 背景
專案需要高可維護性與可測試性

## 決策
採用 Clean Architecture，分為 Domain、Application、Adapter、Infrastructure 四層

## 理由
1. 可測試性：Domain 層獨立，易於單元測試
2. 可維護性：清楚的層級劃分
3. 技術獨立：未來可更換技術

## 後果
- 優點：程式碼品質提升
- 缺點：初期開發較慢
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

## 參考資源

- Clean Architecture (Robert C. Martin)
- Domain-Driven Design (Eric Evans)
- Building Microservices (Sam Newman)
- Design Patterns: Elements of Reusable Object-Oriented Software (GoF)