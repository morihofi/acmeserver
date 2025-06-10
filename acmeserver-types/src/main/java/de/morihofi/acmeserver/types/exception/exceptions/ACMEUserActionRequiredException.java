package de.morihofi.acmeserver.types.exception.exceptions;

import de.morihofi.acmeserver.types.exception.ACMEException;
import de.morihofi.acmeserver.types.exception.objects.ErrorResponse;

/**
 * Exception thrown when user interaction is required before continuing with the ACME workflow.
 */
public class ACMEUserActionRequiredException extends ACMEException {

    /**
     * Message sent back to the client
     */
    private final String message;

    /**
     * Constructs an instance of ACMEUserActionRequiredException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEUserActionRequiredException(String message) {
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
                .type("urn:ietf:params:acme:error:userActionRequired")
                .detail(message)
                .build();
    }
}
