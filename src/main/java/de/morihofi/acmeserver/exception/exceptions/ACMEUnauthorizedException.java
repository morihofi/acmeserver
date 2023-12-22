package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when authentication or authorization is unsuccessful, resulting in unauthorized access.
 */
public class ACMEUnauthorizedException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEUnauthorizedException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEUnauthorizedException(String message) {
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
        response.setType("urn:ietf:params:acme:error:unauthorized");
        response.setDetail(message);
        return response;
    }

}
