package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when the ACME server encounters an internal error or failure.
 */
public class ACMEServerInternalException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEServerInternalException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEServerInternalException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 500;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:serverInternal");
        response.setDetail(message);
        return response;
    }
}
