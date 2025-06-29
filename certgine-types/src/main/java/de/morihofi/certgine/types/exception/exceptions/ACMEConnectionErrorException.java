/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.exceptions;

import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.exception.objects.ErrorResponse;

/**
 * Exception thrown when there is an error while establishing or maintaining a connection with the ACME server.
 */
public class ACMEConnectionErrorException extends ACMEException {

    /**
     * Message sent back to the client
     */
    private final String message;

    /**
     * Constructs an instance of ACMEConnectionErrorException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEConnectionErrorException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 400;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        return ErrorResponse.builder()
                .type("urn:ietf:params:acme:error:connection")
                .detail(message)
                .build();
    }
}
