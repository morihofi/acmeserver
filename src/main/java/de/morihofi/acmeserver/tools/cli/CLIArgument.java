package de.morihofi.acmeserver.tools.cli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class CLIArgument {

    private String parameterName;
    private String prefix;
    private String value;

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    public CLIArgument(String prefix, char valueSplitCharacter, String argument) {
        this.prefix = prefix;

        if(argument.contains(String.valueOf(valueSplitCharacter))){
            int valueSplitCharacterLocation = argument.indexOf(String.valueOf(valueSplitCharacter));

            parameterName = argument.substring(prefix.length(),valueSplitCharacterLocation);
            value = argument.substring(valueSplitCharacterLocation + 1);
        }else{
            parameterName = argument.substring(prefix.length());
            value = null;
        }




    }

    public String getParameterName() {
        return parameterName;
    }

    public String getValue() {
        return value;
    }
}
