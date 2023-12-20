package de.morihofi.acmeserver.tools.password;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

public class SecurePasswordGenerator {
    public static String generateSecurePassword() {

        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase);
        CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase);
        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit);
        CharacterRule specialCharRule = new CharacterRule(EnglishCharacterData.Special);

        // Definieren Sie die minimale Anzahl jedes Zeichentyps
        upperCaseRule.setNumberOfCharacters(3);
        lowerCaseRule.setNumberOfCharacters(3);
        digitRule.setNumberOfCharacters(3);
        specialCharRule.setNumberOfCharacters(3);

        // Generieren Sie das Passwort
        PasswordGenerator generator = new PasswordGenerator();

        // return Passay generated password to the main() method
        return generator.generatePassword(12, upperCaseRule, lowerCaseRule, digitRule, specialCharRule);
    }
}
