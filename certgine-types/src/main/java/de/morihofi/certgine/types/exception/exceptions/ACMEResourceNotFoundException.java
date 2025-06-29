/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.exceptions;

import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.exception.objects.ErrorResponse;

/**
 * Exception thrown when an expected ACME resource is not found.
 */
public class ACMEResourceNotFoundException extends ACMEException {

    /**
     * Message sent back to the client
     */
    private final String message;

    /**
     * Constructs an instance of ACMEResourceNotFoundException with the specified error message.
     *
     * @param message The error message that describes the exception.
     */
    public ACMEResourceNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return 404;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        return ErrorResponse.builder()
                .type("urn:ietf:params:acme:error:notFound")
                .detail(message)
                .build();
    }
}
