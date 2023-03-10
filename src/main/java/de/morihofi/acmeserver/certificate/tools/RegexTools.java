package de.morihofi.acmeserver.certificate.tools;

import java.util.regex.Pattern;

public class RegexTools {

    /**
     * Check, if an email has a valid syntax
     * @param email E-Mail to check
     * @return
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
