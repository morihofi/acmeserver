package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

public class ACMEBadNonceException extends ACMEException {

    private final String message;

    public ACMEBadNonceException(String message) {
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
        response.setType("urn:ietf:params:acme:error:badNonce");
        response.setDetail(message);
        return response;
    }

}