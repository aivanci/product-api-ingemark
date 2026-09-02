package hr.ingemark.assignment.productapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Snake case attributes used as per instructions in the task.
 */
public record ProductResponse(
        Long id,
        String code,
        String name,

        @JsonProperty("price_eur")
        BigDecimal priceEur,

        @JsonProperty("price_usd")
        BigDecimal priceUsd,

        @JsonProperty("is_available")
        boolean available
) {
}
