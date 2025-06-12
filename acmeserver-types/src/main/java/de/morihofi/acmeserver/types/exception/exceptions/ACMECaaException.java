package de.morihofi.acmeserver.types.exception.exceptions;

import de.morihofi.acmeserver.types.exception.ACMEException;
import de.morihofi.acmeserver.types.exception.objects.ErrorResponse;

/**
 * Exception thrown when certificate issuance is blocked by a CAA record.
 */
public class ACMECaaException extends ACMEException {

    private final String message;

    /**
     * Constructs the exception with a message detailing the CAA violation.
     *
     * @param message error description
     */
    public ACMECaaException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 403;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        return ErrorResponse.builder()
                .type("urn:ietf:params:acme:error:caa")
                .detail(message)
                .build();
    }
}
