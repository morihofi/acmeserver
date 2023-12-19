package de.morihofi.acmeserver.postsetup.inputcheck;

public class PortInputChecker implements InputChecker{

    @Override
    public boolean isValid(String input) {
        return input.matches("\\d+") && Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= 65535;
    }
}
