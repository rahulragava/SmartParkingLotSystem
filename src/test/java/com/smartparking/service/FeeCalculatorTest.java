package com.smartparking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartparking.domain.FeeRule;
import com.smartparking.domain.VehicleType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator();
    // 20/hour, 200 daily cap, 15 min grace
    private final FeeRule car = new FeeRule(VehicleType.CAR, new BigDecimal("20.00"), new BigDecimal("200.00"), 15);
    private final Instant entry = Instant.parse("2026-01-01T10:00:00Z");

    private BigDecimal fee(Duration stay) {
        return calculator.calculate(car, entry, entry.plus(stay));
    }

    @Test
    void withinGracePeriodIsFree() {
        assertThat(fee(Duration.ZERO)).isEqualByComparingTo("0");
        assertThat(fee(Duration.ofMinutes(15))).isEqualByComparingTo("0");
    }

    @Test
    void justOverGraceChargesOneHour() {
        assertThat(fee(Duration.ofMinutes(15).plusSeconds(1))).isEqualByComparingTo("20");
        assertThat(fee(Duration.ofMinutes(60))).isEqualByComparingTo("20");
    }

    @Test
    void startedHoursAreRoundedUp() {
        assertThat(fee(Duration.ofMinutes(61))).isEqualByComparingTo("40");
        assertThat(fee(Duration.ofMinutes(180))).isEqualByComparingTo("60");
    }

    @Test
    void remainderIsCappedAtDailyCap() {
        assertThat(fee(Duration.ofHours(10))).isEqualByComparingTo("200");
        assertThat(fee(Duration.ofHours(23).plusMinutes(59))).isEqualByComparingTo("200");
    }

    @Test
    void multiDayStaysChargeCapPerFullDayPlusRemainder() {
        assertThat(fee(Duration.ofHours(24))).isEqualByComparingTo("200");
        assertThat(fee(Duration.ofHours(25))).isEqualByComparingTo("220");
        assertThat(fee(Duration.ofHours(48).plusMinutes(1))).isEqualByComparingTo("420");
    }

    @Test
    void feeIsReturnedWithTwoDecimals() {
        assertThat(fee(Duration.ofHours(2)).scale()).isEqualTo(2);
    }

    @Test
    void exitBeforeEntryIsRejected() {
        assertThatThrownBy(() -> calculator.calculate(car, entry, entry.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
