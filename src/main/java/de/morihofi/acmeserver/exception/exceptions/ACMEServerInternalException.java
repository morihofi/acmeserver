package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

public class ACMEServerInternalException extends ACMEException {

    private final String message;

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
