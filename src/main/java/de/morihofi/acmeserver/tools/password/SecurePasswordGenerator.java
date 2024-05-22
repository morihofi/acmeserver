/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
