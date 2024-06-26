/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.cli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Represents a command-line argument that may have a prefix, a parameter name, and an optional value. For example, in the argument
 * "-port=8080", the prefix is "-", the parameter name is "port", and the value is "8080".
 */
public class CLIArgument {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The name of the parameter (e.g., "port" in "-port=8080")
     */
    private final String parameterName;

    /**
     * The prefix used for the argument (e.g., "-" in "-port=8080")
     */
    private final String prefix;

    /**
     * The optional value associated with the argument (e.g., "8080" in "-port=8080")
     */
    private final String value;

    /**
     * Constructs a CLIArgument object by parsing a command-line argument string.
     *
     * @param prefix              The prefix used for the argument (e.g., "-" in "-port=8080").
     * @param valueSplitCharacter The character used to split the argument into parameter name and value (e.g., '=' in "-port=8080").
     * @param argument            The command-line argument string to parse.
     */
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    public CLIArgument(String prefix, char valueSplitCharacter, String argument) {
        this.prefix = prefix;

        if (argument.contains(String.valueOf(valueSplitCharacter))) {
            int valueSplitCharacterLocation = argument.indexOf(String.valueOf(valueSplitCharacter));

            parameterName = argument.substring(prefix.length(), valueSplitCharacterLocation);
            value = argument.substring(valueSplitCharacterLocation + 1);
        } else {
            parameterName = argument.substring(prefix.length());
            value = null;
        }
    }

    /**
     * Gets the name of the parameter associated with this CLIArgument.
     *
     * @return The parameter name.
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Gets the optional value associated with this CLIArgument.
     *
     * @return The value, or null if no value is present.
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the prefix
     *
     * @return command prefix
     */
    public String getPrefix() {
        return prefix;
    }
}
