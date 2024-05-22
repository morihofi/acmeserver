package de.morihofi.acmeserver.tools.password;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

/**
 * Class for generating secure passwords
 */
public class SecurePasswordGenerator {

    /**
     * Generates a secure random password with a specified length and character requirements.
     *
     * @return A securely generated random password.
     */
    public static String generateSecurePassword() {

        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase);
        CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase);
        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit);

        // Define the minimum number of each character type
        upperCaseRule.setNumberOfCharacters(3);
        lowerCaseRule.setNumberOfCharacters(3);
        digitRule.setNumberOfCharacters(3);

        // Generate the password
        PasswordGenerator generator = new PasswordGenerator();

        // Return the Passay generated password
        return generator.generatePassword(12, upperCaseRule, lowerCaseRule, digitRule);
    }

    private SecurePasswordGenerator() {}
}
