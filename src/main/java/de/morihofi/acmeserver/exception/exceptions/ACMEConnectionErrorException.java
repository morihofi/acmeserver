package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when there is an error while establishing or maintaining a connection with the ACME server.
 */
public class ACMEConnectionErrorException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEConnectionErrorException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEConnectionErrorException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        // TODO: Check if this is correct
        return 403;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:connection");
        response.setDetail(message);
        return response;
    }
}
