package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when a Certificate Signing Request (CSR) provided to ACME is invalid or malformed.
 */
public class ACMEBadCsrException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEBadCsrException with the specified error message.
     * @param message The error message that describes the exception.
     */
    public ACMEBadCsrException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        //TODO: Check if this is the correct code
        return 400;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:badCSR");
        response.setDetail(message);
        return response;
    }

}
