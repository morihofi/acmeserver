package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

public class ACMEMalformedException extends ACMEException {

    private final String message;

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
