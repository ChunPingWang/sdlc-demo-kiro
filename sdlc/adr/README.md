# ADR 決策日誌（Decision Log）

架構決策紀錄（Architecture Decision Record）是 SDLC 中唯一負責記錄「**為什麼這樣決定**」的產物，與其他文件互補：

| 文件 | 回答的問題 |
|------|-----------|
| FSD | 系統要做什麼 |
| SD | 系統怎麼設計 |
| Task List | 要產出哪些程式 |
| **ADR** | **為什麼這樣決定（含被否決的方案）** |
| Steering | 團隊固定怎麼寫 |

---

## ADR 是輸出，HITL 是雙向

ADR 是流程**產出的 artifact**，不是人工手寫餵進去的輸入。架構決策的主體是「人」，AI 不自行拍板：

```
generate-sd 執行
   ① AI 提出候選方案（含 tradeoff）        系統 → 人
   ② 架構師輸入決策 / 補限制              人 → 系統   ← HITL 輸入
   ③ AI 起草 ADR（Status: Proposed）
   ④ 架構師審核 → 核准                    人 → 系統   ← HITL 審核
   ⑤ 輸出 sdlc/adr/output/ADR-NNNN-*.md   最終 artifact
```

已 `Accepted` 的 ADR 若被下游階段（SD 索引、code gen、code review）沿用，屬於「重用既有輸出」，不是重新撰寫輸入。

---

## 狀態生命週期

`Proposed`（AI 起草，待審核）→ `Accepted`（架構師核准）  
→ `Deprecated`（不再適用）/ `Superseded by ADR-NNNN`（被新決策取代）

已 Accepted 的檔案不得直接改決策內容；變更須新開 ADR 並標記取代關係。

---

## 決策索引（LIFE MVP）

| ADR | 標題 | 狀態 | 決策 | 對應 SD |
|-----|------|------|------|--------|
| [ADR-0001](output/ADR-0001-architecture-style.md) | 架構風格 | ✅ Accepted | Modular Monolith | SD §3.3 |
| [ADR-0002](output/ADR-0002-backend-framework.md) | 後端語言與框架 | ✅ Accepted | Java 17 / Spring Boot 3.3 | SD §3.3 |
| [ADR-0003](output/ADR-0003-database.md) | 資料庫選型 | ✅ Accepted | PostgreSQL 15 | SD §3.3 |
| [ADR-0004](output/ADR-0004-cache-strategy.md) | 快取策略 | ✅ Accepted | Redis Cache-Aside | SD §3.3 |
| [ADR-0005](output/ADR-0005-orm.md) | ORM | ✅ Accepted | Spring Data JPA + Hibernate 6 | SD §3.3 |
| [ADR-0006](output/ADR-0006-dto-mapping.md) | DTO 映射 | ✅ Accepted | MapStruct | SD §3.3 |

---

## 目錄

```
sdlc/adr/
├── templates/
│   └── ADR-template.md      # MADR 格式模板（含 Status 生命週期）
├── output/
│   └── ADR-NNNN-*.md        # 各筆決策紀錄
└── README.md                # 本檔：決策索引
```

## 撰寫慣例

- 檔名：`ADR-{4 位數編號}-{kebab-case 標題}.md`，編號流水遞增、不重用
- 一個決策一份檔案；不要把多個決策塞進一份
- 被取代時：舊檔狀態改 `Superseded by ADR-NNNN`，不刪除（保留歷史）
- 當某 ADR 成為長期通則，可晉升進 `.kiro/steering/java-coding-standards.md`
