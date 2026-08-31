package hr.ingemark.assignment.productapi.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ResponseEntity<ApiError> handleExchangeRateUnavailable(ExchangeRateUnavailableException ex, HttpServletRequest request) {
        log.error("HNB exchange rate service unavailable", ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
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
