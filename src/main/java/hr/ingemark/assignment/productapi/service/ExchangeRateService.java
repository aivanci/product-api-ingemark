package hr.ingemark.assignment.productapi.service;

import hr.ingemark.assignment.productapi.client.HnbApiClient;
import hr.ingemark.assignment.productapi.dto.hnb.HnbExchangeRateDto;
import hr.ingemark.assignment.productapi.exception.ExchangeRateUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Converts EUR prices to USD using the Croatian National Bank (HNB) daily middle exchange rate.
 * HNB publishes one exchange rate list per business day, so the fetched list is cached in memory
 * for the current day to avoid hitting the external API on every single product creation. The
 * actual HTTP call - including retry - lives in {@link HnbApiClient}.
 * **/
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String TARGET_CURRENCY = "USD";
    private static final int RATE_SCALE = 6;
    public static final int PRICE_SCALE = 2;

    private final HnbApiClient hnbApiClient;
    private final AtomicReference<CachedRate> cachedRate = new AtomicReference<>();

    public BigDecimal convertEurToUsd(BigDecimal priceEur) {
        BigDecimal usdRate = getUsdRate();
        return priceEur.multiply(usdRate).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal getUsdRate() {
        CachedRate cached = cachedRate.get();
        LocalDate today = LocalDate.now();

        if (cached != null && cached.date().equals(today)) {
            return cached.rate();
        }

        BigDecimal freshRate = fetchUsdRateFromHnb();
        cachedRate.set(new CachedRate(today, freshRate));
        return freshRate;
    }

    private BigDecimal fetchUsdRateFromHnb() {
        List<HnbExchangeRateDto> rates = hnbApiClient.fetchExchangeRateList();
        return extractUsdRate(rates);
    }

    private BigDecimal extractUsdRate(List<HnbExchangeRateDto> rates) {
        return rates.stream()
                .filter(this::isUsdRate)
                .findFirst()
                .map(rate -> parseRate(rate.middleRate()))
                .orElseThrow(() -> new ExchangeRateUnavailableException(
                        "USD exchange rate not found in HNB response"));
    }

    private boolean isUsdRate(HnbExchangeRateDto rate) {
        return TARGET_CURRENCY.equals(rate.currencyCode());
    }

    private BigDecimal parseRate(String rawRate) {
        String normalized = rawRate.replace(',', '.');
        return new BigDecimal(normalized).setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private record CachedRate(LocalDate date, BigDecimal rate) {
    }
}
