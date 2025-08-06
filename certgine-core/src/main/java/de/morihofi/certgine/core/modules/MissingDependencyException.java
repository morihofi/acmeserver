package de.morihofi.certgine.core.modules;

/**
 * Thrown when a module cannot be registered because a required dependency is missing or already registered.
 */
public class MissingDependencyException extends RuntimeException {

    /**
     * Creates a new exception with the provided detail message.
     *
     * @param message description of the missing dependency
     */
    public MissingDependencyException(String message) {
        super(message);
    }
}
