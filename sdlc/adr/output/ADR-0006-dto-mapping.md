# ADR-0006：DTO 映射

---

## 中繼資料

| 欄位 | 內容 |
|------|------|
| **ADR 編號** | ADR-0006 |
| **標題** | DTO 映射策略 |
| **狀態** | `Accepted` |
| **決策日期** | 2026-09-07 |
| **決策者（HITL）** | 系統架構師 |
| **相關需求** | FSD-LIFE-v1.0（分層職責：Service 負責 Entity↔DTO 轉換）|
| **取代** | N/A |

---

## 1. 背景與問題（Context）

依分層規範，Service 層需將 Entity 轉為 DTO（禁止直接回傳 Entity）。需決定 Entity ↔ DTO 的映射方式。

- 業務驅動：需大量重複的物件轉換，手寫易出錯且冗長。
- 技術限制：不希望在請求路徑引入反射造成效能損耗。
- 前提假設：映射多為欄位對應，少數需自訂轉換。

---

## 2. 決策（Decision）

> **我們決定：採用 MapStruct。**

以 MapStruct 於**編譯期**生成映射程式碼，零反射、效能佳，Mapper 介面置於 `mapper` package。

---

## 3. 考量的替代方案（Alternatives）

| 方案 | 優點 | 缺點 | 是否採用 |
|------|------|------|---------|
| **MapStruct** | 編譯期生成、零反射、可讀、可測 | 需 annotation processor 設定 | ✅ 採用 |
| 手寫映射 | 無額外依賴 | 樣板多、易漏欄位、維護成本高 | ❌ 否決：規模化後易出錯 |
| ModelMapper | 設定簡單 | 執行期反射、效能較差 | ❌ 否決：反射損耗 |

---

## 4. 結果與影響（Consequences）

**正面：**
- 編譯期驗證映射完整性，效能無反射損耗。
- Mapper 可獨立測試。

**負面 / 取捨：**
- 需正確設定 Maven annotation processor 順序（與 Lombok 並用時）。

**後續行動：**
- Mapper 命名遵循 `{Resource}Mapper`（見 `java-coding-standards`）。

---

## 5. 關聯

| 類型 | 連結 |
|------|------|
| 對應 SD 章節 | SD-LIFE-v1.0.md §3.3 |
| 對應 FSD 需求 | FSD-LIFE-v1.0.md（分層職責）|
| 相關 ADR | ADR-0002 |
| 影響的程式碼 | `com.example.lifepremium.mapper.*` |
