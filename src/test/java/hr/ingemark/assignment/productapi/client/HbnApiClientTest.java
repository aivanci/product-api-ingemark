package hr.ingemark.assignment.productapi.client;

import hr.ingemark.assignment.productapi.dto.hnb.HnbExchangeRateDto;
import hr.ingemark.assignment.productapi.exception.ExchangeRateUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


@SpringJUnitConfig(classes = HnbApiClientTest.RetryTestConfig.class)
@TestPropertySource(properties = {
        "app.hnb.retry.max-attempts=3",
        "app.hnb.retry.delay-ms=0"
})
class HnbApiClientTest {

    private static final String HNB_URL = "https://api.hnb.hr/tecajn-eur/v3";

    @Autowired
    private HnbApiClient hnbApiClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @BeforeEach
    void resetMockServer() {
        mockServer.reset();
    }

    @Test
    void fetchExchangeRateList_retriesTransientFailuresAndSucceeds() {
        mockServer.expect(times(2), requestTo(HNB_URL)).andRespond(withServerError());
        mockServer.expect(requestTo(HNB_URL))
                .andRespond(withSuccess("""
                        [{"valuta":"USD","srednji_tecaj":"1,164500"}]
                        """, MediaType.APPLICATION_JSON));

        List<HnbExchangeRateDto> rates = hnbApiClient.fetchExchangeRateList();

        assertThat(rates).hasSize(1);
        mockServer.verify();
    }

    @Test
    void fetchExchangeRateList_recoversToExchangeRateUnavailableAfterExhaustingRetries() {
        mockServer.expect(times(3), requestTo(HNB_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> hnbApiClient.fetchExchangeRateList())
                .isInstanceOf(ExchangeRateUnavailableException.class);

        mockServer.verify();
    }

    @Test
    void fetchExchangeRateList_doesNotRetryOnEmptyResponse() {
        mockServer.expect(times(1), requestTo(HNB_URL))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> hnbApiClient.fetchExchangeRateList())
                .isInstanceOf(ExchangeRateUnavailableException.class);

        mockServer.verify();
    }

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        RestClient.Builder hnbRestClientBuilder() {
            return RestClient.builder().baseUrl(HNB_URL);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder hnbRestClientBuilder) {
            return MockRestServiceServer.bindTo(hnbRestClientBuilder).build();
        }

        @Bean
        RestClient hnbRestClient(RestClient.Builder hnbRestClientBuilder, MockRestServiceServer mockRestServiceServer) {
            return hnbRestClientBuilder.build();
        }

        @Bean
        HnbApiClient hnbApiClient(RestClient hnbRestClient) {
            return new HnbApiClient(hnbRestClient);
        }
    }
}
