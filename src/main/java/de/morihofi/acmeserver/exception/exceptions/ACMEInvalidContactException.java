package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when the contact information provided to ACME is considered invalid or unsupported.
 */
public class ACMEInvalidContactException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEInvalidContactException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEInvalidContactException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 403;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:invalidContact");
        response.setDetail(message);
        return response;
    }
}
