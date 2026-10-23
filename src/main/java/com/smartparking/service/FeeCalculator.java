package com.smartparking.service;

import com.smartparking.domain.FeeRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Pure fee logic (no I/O).
 * <ol>
 *   <li>Duration is rounded up to whole minutes.</li>
 *   <li>Stays &le; grace period are free.</li>
 *   <li>Otherwise each started hour is charged at the hourly rate.</li>
 *   <li>Every full 24h block costs the daily cap; the remainder is charged hourly but never above the cap.</li>
 * </ol>
 */
@Component
public class FeeCalculator {

    private static final long MINUTES_PER_DAY = 24 * 60;

    public BigDecimal calculate(FeeRule rule, Instant entry, Instant exit) {
        if (exit.isBefore(entry)) {
            throw new IllegalArgumentException("Exit time is before entry time");
        }
        long seconds = Duration.between(entry, exit).getSeconds();
        long minutes = (seconds + 59) / 60;

        if (minutes <= rule.getGraceMinutes()) {
            return money(BigDecimal.ZERO);
        }

        long fullDays = minutes / MINUTES_PER_DAY;
        long remainderMinutes = minutes % MINUTES_PER_DAY;
        long remainderHours = (remainderMinutes + 59) / 60;

        BigDecimal remainderFee = rule.getHourlyRate().multiply(BigDecimal.valueOf(remainderHours))
                .min(rule.getDailyCap());
        BigDecimal total = rule.getDailyCap().multiply(BigDecimal.valueOf(fullDays)).add(remainderFee);
        return money(total);
    }

    public static long billableMinutes(Instant entry, Instant exit) {
        return (Duration.between(entry, exit).getSeconds() + 59) / 60;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
