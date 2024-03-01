package de.morihofi.acmeserver.tools.cli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents a command-line argument that may have a prefix, a parameter name, and an optional value.
 * For example, in the argument "-port=8080", the prefix is "-", the parameter name is "port", and the value is "8080".
 */
public class CLIArgument {

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
     * @return command prefix
     */
    public String getPrefix() {
        return prefix;
    }
}
