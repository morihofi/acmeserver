package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

public class ACMERateLimitedException extends ACMEException {

    private final String message;

    public ACMERateLimitedException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 429; //Too many requests
    }

    @Override
    public ErrorResponse getErrorResponse() {
        ErrorResponse response = new ErrorResponse();
        response.setType("urn:ietf:params:acme:error:rateLimited");
        response.setDetail(message);
        return response;
    }

}