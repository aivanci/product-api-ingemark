package hr.ingemark.assignment.productapi.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Mirrors the @JsonProperty names on ProductRequest so validation errors are keyed by the
    // wire field names the client actually sent, not the internal Java property names.
    private static final Map<String, String> JSON_FIELD_NAMES = Map.of(
            "priceEur", "price_eur",
            "available", "is_available"
    );

    @ExceptionHandler(DuplicateProductCodeException.class)
    public ResponseEntity<ApiError> handleDuplicateCode(DuplicateProductCodeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = extractFieldErrors(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ResponseEntity<ApiError> handleExchangeRateUnavailable(ExchangeRateUnavailableException ex, HttpServletRequest request) {
        log.error("HNB exchange rate service unavailable", ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while processing request to {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    @ExceptionHandler(InvalidSortPropertyException.class)
    public ResponseEntity<ApiError> handleInvalidSortProperty(InvalidSortPropertyException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    private Map<String, String> extractFieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(jsonFieldName(fieldError.getField()), fieldError.getDefaultMessage());
        }
        return errors;
    }


    private String jsonFieldName(String javaFieldName) {
        return JSON_FIELD_NAMES.getOrDefault(javaFieldName, javaFieldName);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
