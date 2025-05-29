package de.morihofi.acmeserver.types.intf;

public interface INonceManager {
    void checkNonceFromDecodedProtected(String decodedProtected);
}
