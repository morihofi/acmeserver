package de.morihofi.acmeserver.exception;

import de.morihofi.acmeserver.exception.objects.ErrorResponse;

/**
 * An abstract base class for custom exceptions related to the Automated Certificate Management Environment (ACME). ACME exceptions extend
 * {@link IllegalArgumentException}.
 */
public abstract class ACMEException extends IllegalArgumentException {

    /**
     * Constructs a new ACME exception with the specified error message.
     *
     * @param s The error message associated with the exception.
     */
    public ACMEException(String s) {
        super(s);
    }

    /**
     * Get the HTTP status code associated with this exception.
     *
     * @return The HTTP status code that should be sent in the response when this exception is thrown.
     */
    public abstract int getHttpStatusCode();

    /**
     * Get the ErrorResponse associated with this exception.
     *
     * @return An ErrorResponse object containing additional error information.
     */
    public abstract ErrorResponse getErrorResponse();
}
