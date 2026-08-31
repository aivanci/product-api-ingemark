package hr.ingemark.assignment.productapi.exception;

public class DuplicateProductCodeException extends RuntimeException {

    public DuplicateProductCodeException(String message) {
        super(message);
    }
}
