package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when an ACME request or response is malformed or improperly formatted.
 */
public class ACMEMalformedException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEMalformedException with the specified error message.
     * @param message The error message that describes the exception.
     */
    public ACMEMalformedException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        //TODO: Check if correct
        return 400;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:malformed");
        response.setDetail(message);
        return response;
    }

}
