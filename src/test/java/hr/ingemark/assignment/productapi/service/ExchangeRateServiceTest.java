package hr.ingemark.assignment.productapi.service;

import hr.ingemark.assignment.productapi.client.HnbApiClient;
import hr.ingemark.assignment.productapi.dto.hnb.HnbExchangeRateDto;
import hr.ingemark.assignment.productapi.exception.ExchangeRateUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private HnbApiClient hnbApiClient;

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(hnbApiClient);
    }

    @Test
    void convertEurToUsd_usesUsdMiddleRateFromHnbResponse() {
        given(hnbApiClient.fetchExchangeRateList()).willReturn(List.of(
                new HnbExchangeRateDto("AUD", "1,619600"),
                new HnbExchangeRateDto("USD", "1,164500")
        ));

        BigDecimal result = exchangeRateService.convertEurToUsd(new BigDecimal("100.00"));

        assertThat(result).isEqualByComparingTo(new BigDecimal("116.45"));
    }

    @Test
    void convertEurToUsd_cachesRateWithinTheSameDay() {
        given(hnbApiClient.fetchExchangeRateList())
                .willReturn(List.of(new HnbExchangeRateDto("USD", "1,164500")));

        exchangeRateService.convertEurToUsd(new BigDecimal("10.00"));
        exchangeRateService.convertEurToUsd(new BigDecimal("20.00"));

        verify(hnbApiClient, times(1)).fetchExchangeRateList();
    }

    @Test
    void convertEurToUsd_throwsWhenUsdRateIsMissingFromResponse() {
        given(hnbApiClient.fetchExchangeRateList())
                .willReturn(List.of(new HnbExchangeRateDto("AUD", "1,619600")));

        assertThatThrownBy(() -> exchangeRateService.convertEurToUsd(new BigDecimal("10.00")))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
