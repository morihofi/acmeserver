/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.exceptions;

import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.exception.objects.ErrorResponse;

/**
 * Exception thrown when an ACME certificate is attempted to be revoked, but it is already revoked.
 */
public class ACMEAlreadyRevokedException extends ACMEException {

    /**
     * Message sent back to the client
     */
    private final String message;

    /**
     * Constructs an instance of ACMEAlreadyRevokedException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEAlreadyRevokedException(String message) {
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
                .type("urn:ietf:params:acme:error:alreadyRevoked")
                .detail(message)
                .build();
    }
}
