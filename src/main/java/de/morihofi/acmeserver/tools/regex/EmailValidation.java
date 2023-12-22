package de.morihofi.acmeserver.tools.regex;

import java.util.regex.Pattern;

/**
 * Utility class for email address validation.
 * This class provides a method to validate email addresses based on their syntax.
 */
public class EmailValidation {

    private EmailValidation(){}


    /**
     * Check if an email has a valid syntax.
     *
     * @param email The email address to check.
     * @return True if it is a valid email address, otherwise false.
     */
    public static boolean isValidEmail(String email) {
        // Regular expression pattern to match an email address
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        // Compile the pattern
        Pattern pattern = Pattern.compile(emailRegex);

        // Check if the input string matches the pattern
        return pattern.matcher(email).matches();
    }


}
