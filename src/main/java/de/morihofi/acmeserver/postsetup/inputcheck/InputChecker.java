package de.morihofi.acmeserver.postsetup.inputcheck;

/**
 * Functional interface for checking the validity of a string input.
 */
@FunctionalInterface
public interface InputChecker {

    /**
     * Checks whether the given input string is valid.
     *
     * @param input The input string to be checked for validity.
     * @return {@code true} if the input is considered valid, {@code false} otherwise.
     */
    boolean isValid(String input);
}
