package de.morihofi.acmeserver.exception.exceptions;

import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * Exception thrown when the signature algorithm used for an ACME request or response is unsupported or invalid.
 */
public class ACMEBadSignatureAlgorithmException extends ACMEException {

    private final String message;

    /**
     * Constructs an instance of ACMEBadSignatureAlgorithmException with the specified error message.
     * @param message The error message that describes the exception.
     */
    public ACMEBadSignatureAlgorithmException(String message) {
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
        response.setType("urn:ietf:params:acme:error:badSignatureAlgorithm");
        response.setDetail(message);
        return response;
    }

}
