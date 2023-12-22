package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when an ACME certificate is attempted to be revoked, but it is already revoked.
 */
public class ACMEAlreadyRevokedException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEAlreadyRevokedException with the specified error message.
     * @param message The error message that describes the exception.
     */
    public ACMEAlreadyRevokedException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        //TODO: Check if this is correct
        return 400;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:alreadyRevoked");
        response.setDetail(message);
        return response;
    }

}
