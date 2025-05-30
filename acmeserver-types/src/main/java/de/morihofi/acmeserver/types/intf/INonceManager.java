package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.exception.exceptions.ACMEBadNonceException;

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
