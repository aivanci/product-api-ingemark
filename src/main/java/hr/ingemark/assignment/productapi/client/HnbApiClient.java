package hr.ingemark.assignment.productapi.client;

import hr.ingemark.assignment.productapi.dto.hnb.HnbExchangeRateDto;
import hr.ingemark.assignment.productapi.exception.ExchangeRateUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HnbApiClient {

    private final RestClient hnbRestClient;

    @Retryable(
            retryFor = RestClientException.class,
            noRetryFor = ExchangeRateUnavailableException.class,
            notRecoverable = ExchangeRateUnavailableException.class,
            maxAttemptsExpression = "${app.hnb.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${app.hnb.retry.delay-ms}")
    )
    public List<HnbExchangeRateDto> fetchExchangeRateList() {
        List<HnbExchangeRateDto> rates = hnbRestClient.get()
                .retrieve()
                .body(new ParameterizedTypeReference<List<HnbExchangeRateDto>>() {
                });

        if (rates == null || rates.isEmpty()) {
            // Propagates immediately instead of being retried.
            // Retrying won't fix a response the API already returned successfully.
            throw new ExchangeRateUnavailableException("HNB API returned an empty exchange rate list");
        }
        return rates;
    }

    @Recover
    public List<HnbExchangeRateDto> recover(RestClientException ex) {
        log.error("HNB API unreachable after exhausting retries", ex);
        throw new ExchangeRateUnavailableException("Unable to reach HNB exchange rate service after retries", ex);
    }
}
