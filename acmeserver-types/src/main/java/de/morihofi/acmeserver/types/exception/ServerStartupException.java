package de.morihofi.acmeserver.types.exception;

public class ServerStartupException extends RuntimeException {
    public ServerStartupException(String message) {
        super(message);
    }
}
