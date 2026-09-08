---
name: code-review
description: 對 Java/Spring Boot 程式碼進行分層式 Code Review。結構性規則（分層依賴、命名慣例、package 結構、循環依賴、注解規範）交由 ArchUnit 確定性測試檢查，大幅降低 token 消耗；LLM 只審查無法機械化的語意問題（業務邏輯正確性、資安意圖、缺漏驗證）。Use when reviewing Java/Spring Boot code quality, architecture conformance, or security.
metadata:
  author: kiro-sdlc
  version: "1.0"
  stage: Review
  tools: ArchUnit, LLM semantic review
---

## 概述

本 Skill 以「**能機械化的交給 ArchUnit，需要理解的才用 LLM**」為核心原則，  
將 Code Review 拆為兩層，最小化 token 消耗同時最大化涵蓋率。

```
                   Code Review
                        │
        ┌───────────────┴───────────────┐
        ▼                               ▼
  結構性檢查（確定性）              語意性檢查（需理解）
  → ArchUnit 測試                  → LLM 審查
  ─────────────────                ─────────────────
  • 分層依賴方向                    • 業務規則正確性
  • package 命名結構                • 資安意圖（權限、注入、加密）
  • 類別命名慣例                    • 缺漏的輸入驗證
  • 注解規範                        • 錯誤處理完整性
  • 循環依賴                        • 邊界條件與 null 安全
  ─────────────────                ─────────────────
  build 時執行，只回傳 pass/fail    LLM 只讀 ArchUnit 未涵蓋的部分
  ≈ 0 token（不需讀原始碼）         token 限縮在語意層
```

---

## 為何 ArchUnit 能降低 token？

| 對照項 | 純 LLM Review | ArchUnit + LLM |
|--------|--------------|----------------|
| 分層依賴檢查 | 讀取全部 Controller/Service/Repository 原始碼再推理 | 一條 `noClasses().should().dependOnClassesThat()` 規則，build 執行 |
| 命名慣例檢查 | 逐檔比對命名 | 一條 `classes().should().haveSimpleNameEndingWith()` 規則 |
| token 成本 | 隨檔案數線性成長（40 檔 ≈ 數萬 token）| 只讀測試結果（數百 token）|
| 一致性 | 每次結果可能不同 | 確定性，可重現 |
| 執行時機 | 每次 review 重跑 | CI 自動執行，回歸保護 |

**結論：** 把 `java-coding-standards.md` 中所有「結構規則」一次性編碼為 ArchUnit 測試，  
之後每次 review／CI 都由 build 驗證，LLM 只需處理語意層，token 大幅下降。

---

## 執行流程

### ── Phase 1：ArchUnit 結構檢查（確定性，低 token）

#### Step 1-1：確認 ArchUnit 測試存在

檢查 `src/test/java/{package}/architecture/ArchitectureTest.java` 是否存在：

- **存在** → 直接執行
- **不存在** → 依 `references/archunit-rules.md` 產生，規則對照 `.kiro/steering/java-coding-standards.md`

#### Step 1-2：執行 ArchUnit 測試

```bash
# Maven
./mvnw -B -ntp test -Dtest=ArchitectureTest

# Gradle
./gradlew test --tests "*ArchitectureTest"
```

#### Step 1-3：解讀結果

| 結果 | 意義 | 處置 |
|------|------|------|
| 全部通過 | 結構符合規範 | 進入 Phase 2 語意審查 |
| 有失敗 | 結構違規（分層、命名、依賴）| 讀取失敗訊息，列出違規類別與規則，**不需讀取全部原始碼** |

ArchUnit 失敗訊息已明確指出「哪個類別違反哪條規則」，例如：

```
Architecture Violation [Priority: MEDIUM] -
Rule 'controller should not depend on repository' was violated (1 times):
  Method PremiumCalculationController.calculate()
  calls RateEntryRepository.findEffectiveRate() in (PremiumCalculationController.java:42)
```

LLM 直接依此訊息定位修正，無需掃描整個 codebase。

---

### ── Phase 2：LLM 語意審查（僅處理無法機械化的部分）

> 前提：ArchUnit 已通過，結構層無需再看。LLM 只聚焦以下四類語意問題。

#### 2-1 業務規則正確性

- 對照 FSD 功能需求與 SD 業務規則，確認 Service 實作邏輯正確
- 檢查計算公式、狀態轉換、條件分支是否符合 BR 定義
- 範例：保費計算的四捨五入方向、月繳附加費率是否正確套用

#### 2-2 資安意圖（ArchUnit 難以判斷的部分）

| 檢查項 | 說明 |
|--------|------|
| 權限授權 | `@PreAuthorize` 的角色條件是否符合 SD 權限矩陣 |
| 資料隔離 | 查詢是否正確過濾當前使用者（如 Agent 只能看自己的紀錄）|
| 注入風險 | 是否有字串拼接 SQL／JPQL（應使用參數化查詢）|
| 敏感資料 | 密碼、token 是否誤入日誌或回應 DTO |

#### 2-3 缺漏的輸入驗證

- Request DTO 的 Bean Validation 是否涵蓋所有欄位約束
- Service 層業務驗證是否涵蓋 FSD 例外情境
- 邊界值（0、最大值、null、空字串）是否有對應處理

#### 2-4 錯誤處理完整性

- 每個 FSD 例外情境是否有對應的 Exception 與 ErrorCode
- `GlobalExceptionHandler` 是否涵蓋所有 BusinessException 子類別
- 資源不存在、衝突、驗證失敗是否回傳正確 HTTP 狀態碼

---

## Phase 3：產出 Review 報告

產出 `CODE-REVIEW-{PROJECT_CODE}-{DATE}.md` 至專案根目錄：

```markdown
# Code Review 報告

**專案：** {PROJECT_NAME}
**日期：** {DATE}
**範圍：** {commit range / PR / 全專案}

---

## 一、ArchUnit 結構檢查（確定性）

| 規則群 | 結果 | 違規數 |
|--------|------|--------|
| 分層依賴 | ✅ Pass / ❌ Fail | {N} |
| package 命名結構 | ✅ / ❌ | {N} |
| 類別命名慣例 | ✅ / ❌ | {N} |
| 注解規範 | ✅ / ❌ | {N} |
| 循環依賴 | ✅ / ❌ | {N} |

{若有違規，列出 ArchUnit 失敗訊息與修正建議}

---

## 二、LLM 語意審查

### 🔴 Critical（必須修正）
- {問題}｜位置：{檔案:行}｜建議：{修正方式}

### 🟡 Major（建議修正）
- {問題}｜位置｜建議

### 🟢 Minor（可選改善）
- {問題}｜位置｜建議

---

## 三、總結

- ArchUnit：{通過/失敗數}
- 語意問題：Critical {N} / Major {N} / Minor {N}
- 結論：{可合併 / 需修正後再審}
```

---

## 與其他 Skill 的關係

| Skill | 關係 |
|-------|------|
| `springboot-codegen` | code gen 完成後，本 Skill 進行 review |
| `.kiro/steering/java-coding-standards.md` | ArchUnit 規則的來源依據，兩者必須一致 |

---

## 參考資源

- `references/archunit-rules.md` — 對照 java-coding-standards 的 ArchUnit 規則範本
- `.kiro/steering/java-coding-standards.md` — 結構規範來源（ArchUnit 規則須與此一致）
- ArchUnit 官方文件：https://www.archunit.org
