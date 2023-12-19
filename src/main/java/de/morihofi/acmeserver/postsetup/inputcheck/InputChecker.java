package de.morihofi.acmeserver.postsetup.inputcheck;

@FunctionalInterface
public interface InputChecker {
    boolean isValid(String input);
}
