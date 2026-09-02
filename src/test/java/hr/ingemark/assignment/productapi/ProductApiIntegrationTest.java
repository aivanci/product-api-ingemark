package hr.ingemark.assignment.productapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.ingemark.assignment.productapi.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Boots the full application against a real Postgres (Testcontainers), exercising the Flyway
 * migration, the actual schema constraints, and the complete HTTP request/response cycle.
 * <p>
 * The HNB integration: it is a third-party API
 * whose parsing and caching behaviour is covered in isolation by {@code ExchangeRateServiceTest}.
 * A fixed rate of 1.10 keeps assertions deterministic.
 * <p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ProductApiIntegrationTest {

    private static final BigDecimal FIXED_EUR_USD_RATE = new BigDecimal("1.10");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void stubExchangeRate() {
        given(exchangeRateService.convertEurToUsd(any(BigDecimal.class)))
                .willAnswer(this::convertWithFixedRate);
    }

    @Test
    void createAndFetchProduct_roundTripsThroughRealPostgresSchema() throws Exception {
        ResponseEntity<String> createResponse = postProduct("INTTEST001", "Integration Widget", "100.00");

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(createResponse.getBody());
        long id = created.get("id").asLong();
        assertThat(created.get("code").asText()).isEqualTo("INTTEST001");
        assertThat(created.get("price_eur").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(created.get("price_usd").decimalValue()).isEqualByComparingTo("110.00");
        assertThat(created.get("is_available").asBoolean()).isTrue();
        assertThat(createResponse.getHeaders().getLocation())
                .hasToString("/api/v1/products/" + id);

        ResponseEntity<String> getResponse = restTemplate.getForEntity("/api/v1/products/" + id, String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetched = objectMapper.readTree(getResponse.getBody());
        assertThat(fetched.get("price_eur").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(fetched.get("price_usd").decimalValue()).isEqualByComparingTo("110.00");
    }

    @Test
    void createProduct_withScalelessPrice_isNormalizedToTwoDecimals() {
        ResponseEntity<String> createResponse = postProduct("SCALETST01", "Scaleless Widget", "50");

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Asserted on the raw body: JsonNode would parse the number as a double and lose the scale.
        assertThat(createResponse.getBody())
                .contains("\"price_eur\":50.00")
                .contains("\"price_usd\":55.00");
    }

    @Test
    void createProduct_withDuplicateCode_returns409() {
        ResponseEntity<String> first = postProduct("DUPCODE001", "First", "10.00");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = postProduct("DUPCODE001", "Second", "20.00");

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createProduct_withInvalidPayload_returns400WithWireFieldNames() throws Exception {
        ResponseEntity<String> response = postProduct("SHORT", "Widget", "-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("fieldErrors").has("code")).isTrue();
        assertThat(body.get("fieldErrors").has("price_eur")).isTrue();
    }

    @Test
    void getProduct_withUnknownId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/products/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listProducts_returnsPagedEnvelopeWithCreatedProducts() throws Exception {
        assertThat(postProduct("LISTITEM01", "List Widget", "5.00").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/products?size=100", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("content").isArray()).isTrue();
        assertThat(body.get("total_elements").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(extractCodes(body.get("content"))).contains("LISTITEM01");
    }

    @Test
    void listProducts_sortsByWireFieldNameThroughRealQuery() throws Exception {
        assertThat(postProduct("SORTCHEAP1", "Cheap Widget", "1.00").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(postProduct("SORTPRICY1", "Pricy Widget", "9999.00").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/v1/products?sort=price_eur,desc&size=100", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> codes = extractCodes(objectMapper.readTree(response.getBody()).get("content"));
        assertThat(codes.indexOf("SORTPRICY1")).isLessThan(codes.indexOf("SORTCHEAP1"));
    }

    @Test
    void listProducts_withUnknownSortProperty_returns400() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/products?sort=bogus,asc", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<String> postProduct(String code, String name, String priceEur) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"code":"%s","name":"%s","price_eur":%s,"is_available":true}
                """.formatted(code, name, priceEur);
        return restTemplate.postForEntity("/api/v1/products", new HttpEntity<>(body, headers), String.class);
    }

    private BigDecimal convertWithFixedRate(InvocationOnMock invocation) {
        BigDecimal priceEur = invocation.getArgument(0);
        return priceEur.multiply(FIXED_EUR_USD_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> extractCodes(JsonNode products) {
        List<String> codes = new ArrayList<>();
        for (JsonNode product : products) {
            codes.add(product.get("code").asText());
        }
        return codes;
    }
}
