package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when rate limiting restrictions are encountered during ACME requests.
 */
public class ACMERateLimitedException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMERateLimitedException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMERateLimitedException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 429; // Too many requests
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:rateLimited");
        response.setDetail(message);
        return response;
    }
}
