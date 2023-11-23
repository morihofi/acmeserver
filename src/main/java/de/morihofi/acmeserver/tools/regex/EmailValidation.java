package de.morihofi.acmeserver.tools.regex;

import java.util.regex.Pattern;

public class EmailValidation {

    /**
     * Check, if an email has a valid syntax
     *
     * @param email email address to check
     * @return is it a valid email or not
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
