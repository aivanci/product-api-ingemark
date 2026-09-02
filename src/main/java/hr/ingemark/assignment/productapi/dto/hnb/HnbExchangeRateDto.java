package hr.ingemark.assignment.productapi.dto.hnb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Partial mapping of a single entry from the HNB "tecajn-eur/v3" exchange rate list
 * (https://api.hnb.hr/tecajn-eur/v3). Base currency is EUR.
 * {@code srednji_tecaj} is the middle rate expressed as "units of {@code valuta} per 1 EUR".
 * Only the fields this service needs are mapped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HnbExchangeRateDto(

        @JsonProperty("valuta")
        String currencyCode,

        // string using a comma decimal separator, e.g. "1,164500"
        @JsonProperty("srednji_tecaj")
        String middleRate
) {
}
