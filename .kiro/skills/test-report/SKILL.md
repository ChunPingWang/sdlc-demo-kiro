---
name: test-report
description: 執行 Maven/Gradle 測試後，讀取 surefire、cucumber、jacoco 原始報告，彙整成統一的中文測試報告 Markdown。涵蓋 ArchUnit 架構測試、Repository/Service/Controller 測試、Cucumber BDD 情境與覆蓋率。Use when generating a consolidated test report after running the test suite.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: Test
  tools: Maven Surefire, Cucumber, JaCoCo
---

## 概述

本 Skill 在測試執行後，將分散的原始報告彙整為單一份可讀的中文測試報告，  
對照 `springboot-codegen` 的 TDD/BDD 測試分層與 `code-review` 的 ArchUnit 結構檢查。

```
測試執行（Maven/Gradle）
        │
        ▼  產生原始報告
  ┌──────────────────────────────────────────────┐
  │ target/surefire-reports/    → JUnit XML/TXT   │  單元/整合/架構測試
  │ target/cucumber-reports/    → cucumber.json   │  BDD 情境
  │ target/site/jacoco/         → jacoco.xml      │  覆蓋率（選用）
  └──────────────────────────────────────────────┘
        │
        ▼  本 Skill 解析並彙整
  sdlc/test/output/TEST-{PROJECT_CODE}-v{VERSION}.md
        （套用 sdlc/test/templates/TEST-REPORT-template.md）
```

---

## 執行流程

### Step 1：執行測試

若測試尚未執行，先執行並確保報告產生：

```bash
# 完整測試 + 覆蓋率
./mvnw -B verify

# 或僅測試（不含覆蓋率）
./mvnw -B test
```

> Windows 若 Maven Wrapper 因家目錄含空格無法啟動，改用已解壓的 Maven 完整路徑呼叫：
> `& "C:\Users\{user}\.m2\wrapper\dists\apache-maven-{ver}\{hash}\bin\mvn.cmd" -B test`

### Step 2：讀取原始報告

依序讀取以下路徑（存在才讀）：

| 來源 | 路徑 | 解析內容 |
|------|------|---------|
| Surefire XML | `target/surefire-reports/TEST-*.xml` | 每個測試類別的 tests/failures/errors/skipped/time |
| Surefire TXT | `target/surefire-reports/*.txt` | 失敗測試的錯誤訊息與堆疊 |
| Cucumber JSON | `target/cucumber-reports/cucumber.json` | 每個 feature/scenario 的 pass/fail、標籤 |
| JaCoCo XML | `target/site/jacoco/jacoco.xml` | 各 package/class 的行與分支覆蓋率 |

**解析要點：**

- **架構測試**：類別名為 `ArchitectureTest` 的 surefire 結果 → 對應報告第 3 章
- **測試分類**：依測試類別的 package 或命名（`*RepositoryTest` / `*ServiceTest` / `*ControllerTest`）歸類
- **Cucumber 標籤統計**：從 json 的 `tags` 欄位彙整 @smoke/@regression/@boundary/@error-handling
- **失敗明細**：只擷取 failure/error 的類別、方法、訊息摘要（不貼完整堆疊）

### Step 3：套用模板產出報告

- 模板：`sdlc/test/templates/TEST-REPORT-template.md`
- 輸出：`sdlc/test/output/TEST-{PROJECT_CODE}-v{VERSION}.md`
- 所有 `{PLACEHOLDER}` 依解析結果替換；無資料的區段標注 `N/A`（如未啟用 JaCoCo）

### Step 4：呈現摘要

在對話中回報執行摘要：

```
📊 測試報告已產出：sdlc/test/output/TEST-{PROJECT_CODE}-v{VERSION}.md

  總測試：{N}｜通過 {N}｜失敗 {N}｜錯誤 {N}｜通過率 {N}%

  ✅ 架構測試（ArchUnit）：  {N}/{N}
  ✅ Repository 測試：       {N}/{N}
  ✅ Service 測試：          {N}/{N}
  ✅ Controller 測試：       {N}/{N}
  ✅ Cucumber BDD：          {N}/{N}
  覆蓋率（整體）：           {N}%

  結論：{✅ 全數通過 / ❌ 有 N 項失敗需修正}
```

若有失敗，列出失敗的測試類別與方法，並建議下一步（如觸發 `code-review` 或修正實作）。

---

## 解析對照表

| 報告章節 | 原始來源 | 解析欄位 |
|---------|---------|---------|
| §1 執行摘要 | 所有 surefire XML 加總 | tests, failures, errors, skipped, time |
| §2 測試分類 | surefire XML 依類別歸類 | 各類型測試數與結果 |
| §3 架構測試 | `TEST-*.ArchitectureTest.xml` | 各 @ArchTest 規則 pass/fail |
| §4 BDD 情境 | `cucumber.json` | feature/scenario/tags |
| §5 失敗明細 | surefire `*.txt` | 失敗類別、方法、訊息 |
| §6 覆蓋率 | `jacoco.xml` | line/branch coverage per package |

---

## 與其他 Skill 的關係

| Skill | 關係 |
|-------|------|
| `springboot-codegen` | code gen 完成後執行測試，本 Skill 彙整結果 |
| `code-review` | ArchUnit 結果同時餵入本報告第 3 章與 code-review |
| `markdown-to-word` | 測試報告可再轉為 Word 交付 |

---

## 參考資源

- 模板：`sdlc/test/templates/TEST-REPORT-template.md`
- 輸出目錄：`sdlc/test/output/`
- Surefire：`target/surefire-reports/`
- Cucumber：`target/cucumber-reports/`
- JaCoCo：`target/site/jacoco/`
