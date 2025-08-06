/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.security;

import de.morihofi.certgine.types.exception.exceptions.ACMEBadNonceException;

public interface INonceManager {
    /**
     * Checks if a nonce from a decoded protected request body has already been used.
     * If the nonce has been used, an ACMEBadNonceException is thrown.
     *
     * @param decodedProtected The decoded protected request body as a JSON string.
     * @throws ACMEBadNonceException If the nonce has already been used.
     */
    void checkNonceFromDecodedProtected(String decodedProtected);
}
