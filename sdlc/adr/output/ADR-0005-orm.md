# ADR-0005：ORM 選型

---

## 中繼資料

| 欄位 | 內容 |
|------|------|
| **ADR 編號** | ADR-0005 |
| **標題** | ORM 選型 |
| **狀態** | `Accepted` |
| **決策日期** | 2026-09-07 |
| **決策者（HITL）** | 系統架構師 |
| **相關需求** | FSD-LIFE-v1.0（資料存取層）|
| **取代** | N/A |

---

## 1. 背景與問題（Context）

需選定資料存取技術，作為 Repository 層的實作基礎，並與 Spring Boot 3.3 相容。

- 業務驅動：需快速開發 CRUD 與複雜查詢。
- 技術限制：Spring Boot 3.x 採 Jakarta EE 9+ 命名空間。
- 前提假設：多數查詢可由 JPQL 表達，複雜查詢以 `@Query` 補足。

---

## 2. 決策（Decision）

> **我們決定：採用 Spring Data JPA + Hibernate 6。**

以 Spring Data JPA 提供 Repository 抽象，Hibernate 6 為 JPA 實作（支援 Jakarta EE 9），複雜查詢使用 JPQL `@Query`，禁止 Native SQL（除非有充分理由）。

---

## 3. 考量的替代方案（Alternatives）

| 方案 | 優點 | 缺點 | 是否採用 |
|------|------|------|---------|
| **Spring Data JPA + Hibernate 6** | 企業標準、Repository 抽象、與 Spring 整合佳、Jakarta EE 9 相容 | 複雜查詢需留意 N+1 | ✅ 採用 |
| MyBatis | SQL 完全掌控 | 樣板多、失去 ORM 抽象 | ❌ 否決：CRUD 開發較慢 |
| jOOQ | 型別安全 SQL | 學習成本、授權考量 | ❌ 否決：MVP 不需要 |

---

## 4. 結果與影響（Consequences）

**正面：**
- 快速開發、與 Spring 生態一致。
- Repository 命名慣例可由 Steering 規範。

**負面 / 取捨：**
- 需以測試與 code review 防範 N+1 查詢。

**後續行動：**
- Repository 複雜查詢統一用 `@Query` + JPQL（見 `java-coding-standards`）。

---

## 5. 關聯

| 類型 | 連結 |
|------|------|
| 對應 SD 章節 | SD-LIFE-v1.0.md §3.3 |
| 對應 FSD 需求 | FSD-LIFE-v1.0.md（資料存取層）|
| 相關 ADR | ADR-0002, ADR-0003 |
| 影響的程式碼 | `com.example.lifepremium.repository.*` |
