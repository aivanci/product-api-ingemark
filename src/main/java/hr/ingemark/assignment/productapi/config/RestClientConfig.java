package hr.ingemark.assignment.productapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient hnbRestClient(
            @Value("${app.hnb.base-url}") String baseUrl,
            @Value("${app.hnb.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.hnb.read-timeout-ms}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = buildRequestFactory(connectTimeoutMs, readTimeoutMs);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private SimpleClientHttpRequestFactory buildRequestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return requestFactory;
    }
}
