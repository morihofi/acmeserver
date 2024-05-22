package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when an identifier (e.g., domain name) is rejected or not accepted by the ACME server.
 */
public class ACMERejectedIdentifierException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMERejectedIdentifierException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMERejectedIdentifierException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 400;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:rejectedIdentifier");
        response.setDetail(message);
        return response;
    }
}
