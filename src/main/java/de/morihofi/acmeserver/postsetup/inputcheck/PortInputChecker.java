package de.morihofi.acmeserver.postsetup.inputcheck;

/**
 * A utility class for checking the validity of port number input strings.
 * It implements the {@link InputChecker} functional interface to provide custom input validation.
 */
public class PortInputChecker implements InputChecker {

    /**
     * Checks whether the given input string is a valid port number.
     *
     * @param input The input string to be checked for validity as a port number.
     * @return {@code true} if the input is a valid port number (0-65535), {@code false} otherwise.
     */
    @Override
    public boolean isValid(String input) {
        return input.matches("\\d+") && Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= 65535;
    }
}
