package hr.ingemark.assignment.productapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope. Spring's {@code PageImpl} is deliberately not serialized directly -
 * its JSON structure is an internal detail and not a stable API contract.
 *
 * Not using PagedModel (possible and suggested Spring solution), since the convention we are using
 * in this project is snake_case, as opposed to Spring's PagedModel camelCase.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,

        @JsonProperty("total_elements")
        long totalElements,

        @JsonProperty("total_pages")
        int totalPages
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
