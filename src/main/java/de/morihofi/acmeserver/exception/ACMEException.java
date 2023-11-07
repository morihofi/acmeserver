package de.morihofi.acmeserver.exception;

import de.morihofi.acmeserver.exception.objects.ErrorResponse;

public abstract class ACMEException extends IllegalArgumentException {
    public ACMEException(String s) {
        super(s);
    }

    public abstract int getHttpStatusCode();

    public abstract ErrorResponse getErrorResponse();

}
