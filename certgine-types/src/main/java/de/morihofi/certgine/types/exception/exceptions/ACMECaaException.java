/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.exceptions;

import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.exception.objects.ErrorResponse;

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
