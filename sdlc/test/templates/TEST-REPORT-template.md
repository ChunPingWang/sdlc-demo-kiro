# 測試報告 (Test Report)

**文件編號：** TEST-{PROJECT_CODE}-{VERSION}  
**專案名稱：** {PROJECT_NAME}  
**測試日期：** {DATE}  
**測試環境：** {local / CI / staging}  
**執行者：** {NAME / CI Pipeline}  
**建置工具：** Maven {version} / JDK {version}  

---

## 1. 執行摘要

| 指標 | 數值 |
|------|------|
| 總測試數 | {N} |
| 通過 | {N} ✅ |
| 失敗 | {N} ❌ |
| 錯誤 | {N} ⚠️ |
| 略過 | {N} ⏭️ |
| 通過率 | {N}% |
| 總執行時間 | {N} 秒 |

**整體結論：** {✅ 全數通過 / ❌ 有失敗需修正}

---

## 2. 測試分類結果

> 對應 `springboot-codegen` 的 TDD/BDD 測試分層與 `code-review` 的 ArchUnit 結構檢查。

| 測試類型 | 測試框架 | 測試數 | 通過 | 失敗 | 來源報告 |
|---------|---------|-------|------|------|---------|
| 架構測試（ArchUnit）| ArchUnit + JUnit5 | {N} | {N} | {N} | `target/surefire-reports/` |
| Repository 測試 | @DataJpaTest | {N} | {N} | {N} | `target/surefire-reports/` |
| Service 單元測試 | JUnit5 + Mockito | {N} | {N} | {N} | `target/surefire-reports/` |
| Controller 整合測試 | @SpringBootTest + MockMvc | {N} | {N} | {N} | `target/surefire-reports/` |
| BDD 情境測試 | Cucumber | {N} | {N} | {N} | `target/cucumber-reports/` |

---

## 3. 架構測試（ArchUnit）明細

> 來源：`target/surefire-reports/TEST-{package}.architecture.ArchitectureTest.xml`  
> 結構性規範由 ArchUnit 確定性驗證，對照 `.kiro/steering/java-coding-standards.md`。

| 規則 | 對照規範 | 結果 |
|------|---------|------|
| 分層架構依賴方向 | §4 分層職責 | ✅ / ❌ |
| Controller 不得直接依賴 Repository | §1.3、§4 | ✅ / ❌ |
| Domain 不得依賴 Spring 框架 | §1.3、§4 | ✅ / ❌ |
| DTO 不得依賴 JPA | §1.3 | ✅ / ❌ |
| Service 不得反向依賴 Controller | §4 | ✅ / ❌ |
| 類別命名慣例（Controller/Repository/ServiceImpl/Exception/Config）| §2 | ✅ / ❌ |
| 注解規範（@RestController / @Service / Repository 介面）| §5 | ✅ / ❌ |
| 套件之間無循環依賴 | §4 | ✅ / ❌ |
| 禁止 field 注入 | §5 | ✅ / ❌ |

**違規明細（若有）：**

```
{貼上 ArchUnit 失敗訊息，格式如下}
Architecture Violation [Priority: MEDIUM] -
Rule '...' was violated (N times):
  {類別} calls {類別} in ({檔案}:{行})
```

---

## 4. BDD 情境測試（Cucumber）明細

> 來源：`target/cucumber-reports/cucumber.json` / `cucumber.html`

| Feature | Scenario 總數 | 通過 | 失敗 | 對應 FR |
|---------|-------------|------|------|--------|
| {feature-name}.feature | {N} | {N} | {N} | {FR_IDS} |

**依標籤統計：**

| 標籤 | Scenario 數 | 通過率 |
|------|-----------|--------|
| @smoke | {N} | {N}% |
| @regression | {N} | {N}% |
| @boundary | {N} | {N}% |
| @error-handling | {N} | {N}% |

**失敗情境（若有）：**

| Scenario | 失敗步驟 | 錯誤訊息 |
|----------|---------|---------|
| {scenario 名稱} | {Given/When/Then 步驟} | {assertion 失敗摘要} |

---

## 5. 失敗與錯誤明細

> 來源：`target/surefire-reports/*.txt`

| # | 測試類別 | 測試方法 | 類型 | 錯誤摘要 |
|---|---------|---------|------|---------|
| 1 | {ClassNameTest} | {methodName} | 失敗 / 錯誤 | {expected vs actual / exception} |

---

## 6. 測試覆蓋率（若啟用 JaCoCo）

> 來源：`target/site/jacoco/index.html`

| 層次 | 行覆蓋率 | 分支覆蓋率 | 目標 | 達標 |
|------|---------|-----------|------|------|
| Service（業務規則）| {N}% | {N}% | ≥ 90% | ✅ / ❌ |
| Controller | {N}% | {N}% | ≥ 80% | ✅ / ❌ |
| Repository | {N}% | {N}% | ≥ 80% | ✅ / ❌ |
| 整體 | {N}% | {N}% | ≥ 80% | ✅ / ❌ |

**未覆蓋的重點方法（若低於目標）：**

- {類別.方法}：{未覆蓋原因/建議補測項目}

---

## 7. 執行指令紀錄

```bash
# 架構測試（ArchUnit）
./mvnw -B test -Dtest=ArchitectureTest

# 完整測試套件
./mvnw -B test

# 含覆蓋率報告
./mvnw -B verify
```

---

## 8. 結論與後續

- **可否進入下一階段：** {✅ 可 / ❌ 需修正後重測}
- **待修正項目：**
  1. {項目}
- **建議：**
  1. {建議，如：補強某 FR 的邊界值測試}

---

*本報告由 Kiro SDLC `test-report` 產生，對照 surefire / cucumber / jacoco 原始報告。*
