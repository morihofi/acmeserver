/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.exceptions;

import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.exception.objects.ErrorResponse;

/**
 * Exception thrown when the signature algorithm used for an ACME request or response is unsupported or invalid.
 */
public class ACMEBadSignatureAlgorithmException extends ACMEException {

    /**
     * Message sent back to the client
     */
    private final String message;

    /**
     * Constructs an instance of ACMEBadSignatureAlgorithmException with the specified error message.
     *
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
        return ErrorResponse.builder()
                .type("urn:ietf:params:acme:error:badSignatureAlgorithm")
                .detail(message)
                .build();
    }
}
