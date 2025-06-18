package de.morihofi.acmeserver.types.server;

/**
 * Custom flag enum that change behaviour of the server
 */
public enum StartupFlag {
    /**
     * Enables the async certificate issuing, that is currently a buggy in certbot.
     */
    USE_ASYNC_CERTIFICATE_ISSUING
}
