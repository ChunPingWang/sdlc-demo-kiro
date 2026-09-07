# encoding: UTF-8
# language: zh-TW

@premium-calculation @smoke
Feature: 壽險保費試算
  作為 壽險業務員或訪客
  我希望能夠 輸入被保人基本資料與商品條件進行保費試算
  以便 即時取得年繳與月繳保費報價，協助客戶投保決策

  Background:
    Given 系統存在商品代碼 "LIFE-WL-01" 的生效費率表
    And 費率表包含以下資料：
      | 年齡 | 性別 | 繳費年期 | 費率   |
      | 35   | M    | 20       | 12.50  |
      | 35   | F    | 20       | 11.80  |
      | 0    | M    | 20       | 5.20   |
      | 70   | M    | 20       | 98.50  |
      | 35   | M    | 10       | 18.90  |
      | 35   | M    | 30       | 9.30   |
      | 35   | M    | 99       | 8.10   |

  # ──────────────────────────────────────────
  # 正常流程（Happy Path）
  # ──────────────────────────────────────────

  @happy-path
  Scenario: 業務員以合法參數試算男性 20 年期保費
    # 對應 FR-CALC-001 主要流程、FR-CALC-002
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求：
      | 欄位         | 值         |
      | productCode  | LIFE-WL-01 |
      | age          | 35         |
      | gender       | M          |
      | insuredAmount| 1000       |
      | paymentPeriod| 20         |
    Then 系統應回傳 HTTP 狀態碼 200
    And 回應中 "annualPremium" 應為 125000
    And 回應中 "monthlyPremium" 應為 10729
    And 回應中 "productCode" 應為 "LIFE-WL-01"
    And 業務員 "A001" 的試算紀錄應被保存

  @happy-path
  Scenario: 業務員試算女性保費（費率不同）
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求：
      | 欄位         | 值         |
      | productCode  | LIFE-WL-01 |
      | age          | 35         |
      | gender       | F          |
      | insuredAmount| 1000       |
      | paymentPeriod| 20         |
    Then 系統應回傳 HTTP 狀態碼 200
    And 回應中 "annualPremium" 應為 118000
    And 回應中 "monthlyPremium" 應為 10137

  @happy-path
  Scenario: 訪客匿名試算（不保存紀錄）
    # 對應 FR-CALC-001 替代流程
    Given 使用者未登入
    When 使用者送出匿名保費試算請求：
      | 欄位         | 值         |
      | productCode  | LIFE-WL-01 |
      | age          | 35         |
      | gender       | M          |
      | insuredAmount| 500        |
      | paymentPeriod| 20         |
    Then 系統應回傳 HTTP 狀態碼 200
    And 回應中 "annualPremium" 應為 62500
    And 系統試算紀錄表中不應新增任何紀錄

  # ──────────────────────────────────────────
  # 邊界值測試（Boundary）
  # ──────────────────────────────────────────

  @boundary @regression
  Scenario Outline: 年齡邊界值試算
    # 對應 BR-001 年齡限制
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求，年齡為 <年齡>，性別 "M"，保額 1000 萬，繳費年期 20，商品 "LIFE-WL-01"
    Then 系統應回傳 HTTP 狀態碼 <預期狀態碼>

    Examples:
      | 年齡 | 預期狀態碼 |
      | 0    | 200        |
      | 70   | 200        |
      | 71   | 422        |
      | -1   | 422        |

  @boundary @regression
  Scenario Outline: 保額邊界值試算
    # 對應 BR-002 保額限制（單位：萬元）
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求，年齡 35，性別 "M"，保額 <保額> 萬，繳費年期 20，商品 "LIFE-WL-01"
    Then 系統應回傳 HTTP 狀態碼 <預期狀態碼>

    Examples:
      | 保額 | 預期狀態碼 |
      | 100  | 200        |
      | 5000 | 200        |
      | 99   | 422        |
      | 5001 | 422        |
      | 0    | 422        |

  # ──────────────────────────────────────────
  # 錯誤處理（Error Handling）
  # ──────────────────────────────────────────

  @error-handling @regression
  Scenario: 年齡超出上限應回傳 422
    # 對應 FR-CALC-001 例外處理 - AGE_OUT_OF_RANGE
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求，年齡為 71，性別 "M"，保額 1000 萬，繳費年期 20，商品 "LIFE-WL-01"
    Then 系統應回傳 HTTP 狀態碼 422
    And 回應中 "code" 應為 "AGE_OUT_OF_RANGE"
    And 回應中 "message" 應包含 "被保人年齡須介於 0 至 70 歲"

  @error-handling @regression
  Scenario: 保額低於下限應回傳 422
    # 對應 FR-CALC-001 例外處理 - AMOUNT_OUT_OF_RANGE
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求，年齡為 35，性別 "M"，保額 50 萬，繳費年期 20，商品 "LIFE-WL-01"
    Then 系統應回傳 HTTP 狀態碼 422
    And 回應中 "code" 應為 "AMOUNT_OUT_OF_RANGE"

  @error-handling @regression
  Scenario: 繳費年期非合法值應回傳 422
    # 對應 FR-CALC-001 例外處理 - INVALID_PAYMENT_PERIOD
    Given 業務員 "A001" 已登入系統
    When 業務員送出保費試算請求，年齡為 35，性別 "M"，保額 1000 萬，繳費年期 15，商品 "LIFE-WL-01"
    Then 系統應回傳 HTTP 狀態碼 422
    And 回應中 "code" 應為 "INVALID_PAYMENT_PERIOD"

  @error-handling @regression
  Scenario: 查無費率資料應回傳 404
    # 對應 FR-CALC-001 例外處理 - RATE_NOT_FOUND
    Given 業務員 "A001" 已登入系統
    And 系統中不存在商品代碼 "LIFE-XX-99" 的費率資料
    When 業務員送出保費試算請求，商品代碼為 "LIFE-XX-99"，年齡 35，性別 "M"，保額 1000 萬，繳費年期 20
    Then 系統應回傳 HTTP 狀態碼 404
    And 回應中 "code" 應為 "RATE_NOT_FOUND"

  # ──────────────────────────────────────────
  # 月繳保費計算驗證（BR-005）
  # ──────────────────────────────────────────

  @regression
  Scenario Outline: 月繳保費計算公式驗證
    # 月繳 = ROUND(年繳 / 12 × 1.03)
    Given 業務員 "A001" 已登入系統
    And 費率表中年齡 <年齡>、性別 "<性別>"、繳費年期 <繳費年期> 的費率為 <費率>
    When 業務員試算保額 <保額> 萬元
    Then 年繳保費應為 <年繳>
    And 月繳保費應為 <月繳>

    Examples:
      | 年齡 | 性別 | 繳費年期 | 費率  | 保額 | 年繳    | 月繳  |
      | 35   | M    | 20       | 12.50 | 1000 | 125000  | 10729 |
      | 35   | F    | 20       | 11.80 | 1000 | 118000  | 10137 |
      | 35   | M    | 10       | 18.90 | 500  | 94500   | 8115  |
