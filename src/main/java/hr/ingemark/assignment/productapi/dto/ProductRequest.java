package hr.ingemark.assignment.productapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Snake case attributes used as per instructions in the task.
 */
public record ProductRequest(

        @NotBlank(message = "code must not be blank")
        @Size(min = 10, max = 10, message = "code must be exactly 10 characters")
        String code,

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotNull(message = "price_eur is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price_eur must be greater than or equal to 0")
        // integer = 9 (not 10, the column limit) so the derived price_usd (price_eur x rate)
        // also stays within NUMERIC(12,2) for any realistic EUR/USD rate
        @Digits(integer = 9, fraction = 2, message = "price_eur must have at most 9 integer digits and 2 decimal places")
        @JsonProperty("price_eur")
        BigDecimal priceEur,

        @NotNull(message = "is_available is required")
        @JsonProperty("is_available")
        Boolean available
) {
}
