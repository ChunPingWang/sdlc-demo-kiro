package com.example.lifepremium.service;

// FR: FR-CALC-001, FR-CALC-002 | BR: BR-001 ~ BR-005

import com.example.lifepremium.dto.request.PremiumCalculateRequest;
import com.example.lifepremium.dto.response.PremiumCalculateResponse;
import com.example.lifepremium.exception.*;
import com.example.lifepremium.fixture.PremiumFixture;
import com.example.lifepremium.repository.CalculationRecordRepository;
import com.example.lifepremium.repository.RateEntryRepository;
import com.example.lifepremium.service.impl.PremiumCalculationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.example.lifepremium.fixture.PremiumFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PremiumCalculationService Unit Tests")
class PremiumCalculationServiceTest {

    @Mock  private RateEntryRepository         rateEntryRepository;
    @Mock  private CalculationRecordRepository  calculationRecordRepository;
    @InjectMocks
    private PremiumCalculationServiceImpl service;

    @BeforeEach
    void stubRateRepo() {
        given(rateEntryRepository.findEffectiveRate(
                eq(PRODUCT_CODE), eq(DEFAULT_AGE), eq(GENDER_MALE),
                eq(DEFAULT_PERIOD), any(LocalDate.class)))
            .willReturn(Optional.of(MALE_RATE));
    }

    // ── Happy Path ──

    @Nested @DisplayName("calculate() — Happy Path")
    class HappyPath {

        @Test @DisplayName("業務員試算男性 20 年期 → 年繳 125,000, 月繳 10,729")
        // FR-CALC-001 主要流程, BR-004, BR-005
        void male20yr_success() {
            PremiumCalculateResponse resp = service.calculate(aValidRequest(), AGENT_ID);

            assertThat(resp.annualPremium()).isEqualTo(ANNUAL_MALE);
            assertThat(resp.monthlyPremium()).isEqualTo(MONTHLY_MALE);
            assertThat(resp.rateUsed()).isEqualByComparingTo(MALE_RATE);
            then(calculationRecordRepository).should().save(any());
        }

        @Test @DisplayName("訪客試算 → 不保存試算紀錄")
        // FR-CALC-001 替代流程
        void guest_noRecord() {
            service.calculate(aValidRequest(), null);

            then(calculationRecordRepository).should(never()).save(any());
        }

        @Test @DisplayName("女性費率 11.80 → 年繳 118,000, 月繳 10,137")
        // BR-005 月繳公式
        void female_differentRate() {
            given(rateEntryRepository.findEffectiveRate(
                    eq(PRODUCT_CODE), eq(DEFAULT_AGE), eq(GENDER_FEMALE),
                    eq(DEFAULT_PERIOD), any(LocalDate.class)))
                .willReturn(Optional.of(FEMALE_RATE));

            PremiumCalculateRequest req = new PremiumCalculateRequest(
                    PRODUCT_CODE, DEFAULT_AGE, GENDER_FEMALE, DEFAULT_AMOUNT, DEFAULT_PERIOD);
            PremiumCalculateResponse resp = service.calculate(req, AGENT_ID);

            assertThat(resp.annualPremium()).isEqualTo(118000);
            assertThat(resp.monthlyPremium()).isEqualTo(10137);
        }
    }

    // ── BR-001 年齡驗證 ──

    @Nested @DisplayName("BR-001：年齡驗證")
    class AgeValidation {

        @Test @DisplayName("年齡 0 應通過（下限邊界）")
        void age_lowerBoundary_pass() {
            given(rateEntryRepository.findEffectiveRate(
                    anyString(), eq(0), anyString(), anyInt(), any()))
                .willReturn(Optional.of(new BigDecimal("5.20")));
            assertThatCode(() -> service.calculate(aRequestWithAge(0), AGENT_ID))
                .doesNotThrowAnyException();
        }

        @Test @DisplayName("年齡 70 應通過（上限邊界）")
        void age_upperBoundary_pass() {
            given(rateEntryRepository.findEffectiveRate(
                    anyString(), eq(70), anyString(), anyInt(), any()))
                .willReturn(Optional.of(new BigDecimal("98.50")));
            assertThatCode(() -> service.calculate(aRequestWithAge(70), AGENT_ID))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "年齡 {0} 應拋出 AgeOutOfRangeException")
        @ValueSource(ints = {71, 100, -1})
        @DisplayName("超出範圍應拋出 AgeOutOfRangeException")
        void age_outOfRange_throws(int age) {
            assertThatThrownBy(() -> service.calculate(aRequestWithAge(age), AGENT_ID))
                .isInstanceOf(AgeOutOfRangeException.class)
                .hasMessageContaining("被保人年齡須介於 0 至 70 歲");
        }
    }

    // ── BR-002 保額驗證 ──

    @Nested @DisplayName("BR-002：保額驗證")
    class AmountValidation {

        @ParameterizedTest(name = "保額 {0} 萬應通過")
        @ValueSource(ints = {100, 5000})
        void amount_boundary_pass(int amount) {
            assertThatCode(() -> service.calculate(aRequestWithAmount(amount), AGENT_ID))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "保額 {0} 萬應拋出 AmountOutOfRangeException")
        @ValueSource(ints = {99, 5001, 0})
        void amount_outOfRange_throws(int amount) {
            assertThatThrownBy(() -> service.calculate(aRequestWithAmount(amount), AGENT_ID))
                .isInstanceOf(AmountOutOfRangeException.class);
        }
    }

    // ── BR-003 繳費年期驗證 ──

    @Nested @DisplayName("BR-003：繳費年期驗證")
    class PaymentPeriodValidation {

        @ParameterizedTest(name = "繳費年期 {0} 應通過")
        @ValueSource(ints = {10, 20, 30, 99})
        void period_valid_pass(int period) {
            given(rateEntryRepository.findEffectiveRate(
                    anyString(), anyInt(), anyString(), eq(period), any()))
                .willReturn(Optional.of(MALE_RATE));
            assertThatCode(() -> service.calculate(aRequestWithPeriod(period), AGENT_ID))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "繳費年期 {0} 應拋出 InvalidPaymentPeriodException")
        @ValueSource(ints = {15, 25, 0, 100})
        void period_invalid_throws(int period) {
            assertThatThrownBy(() -> service.calculate(aRequestWithPeriod(period), AGENT_ID))
                .isInstanceOf(InvalidPaymentPeriodException.class);
        }
    }

    // ── 費率查無資料 ──

    @Test @DisplayName("查無費率應拋出 RateNotFoundException")
    // FR-CALC-001 例外處理 - RATE_NOT_FOUND
    void rateNotFound_throws() {
        given(rateEntryRepository.findEffectiveRate(anyString(), anyInt(), anyString(), anyInt(), any()))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(
                aRequestWithProductCode("LIFE-XX-99"), AGENT_ID))
            .isInstanceOf(RateNotFoundException.class);
    }
}
