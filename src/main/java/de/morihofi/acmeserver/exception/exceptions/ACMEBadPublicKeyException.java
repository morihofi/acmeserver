package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when a public key provided to ACME is considered invalid or unsupported.
 */
public class ACMEBadPublicKeyException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEBadPublicKeyException with the specified error message.
     * @param message The error message that describes the exception.
     */
    public ACMEBadPublicKeyException(String message) {
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
        response.setType("urn:ietf:params:acme:error:badPublicKey");
        response.setDetail(message);
        return response;
    }

}
