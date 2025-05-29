package de.morihofi.acmeserver.utils.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CLIArgumentTest {

    @Test
    void testArgumentWithValue() {
        CLIArgument arg = new CLIArgument("-", '=', "-port=8080");
        assertEquals("-", arg.getPrefix());
        assertEquals("port", arg.getParameterName());
        assertEquals("8080", arg.getValue());
    }

    @Test
    void testArgumentWithoutValue() {
        CLIArgument arg = new CLIArgument("-", '=', "-help");
        assertEquals("-", arg.getPrefix());
        assertEquals("help", arg.getParameterName());
        assertNull(arg.getValue());
    }

    @Test
    void testArgumentWithDoubleDashPrefix() {
        CLIArgument arg = new CLIArgument("--", '=', "--debug=true");
        assertEquals("--", arg.getPrefix());
        assertEquals("debug", arg.getParameterName());
        assertEquals("true", arg.getValue());
    }

    @Test
    void testArgumentWithColonSplitCharacter() {
        CLIArgument arg = new CLIArgument("/", ':', "/config:path/to/file");
        assertEquals("/", arg.getPrefix());
        assertEquals("config", arg.getParameterName());
        assertEquals("path/to/file", arg.getValue());
    }

    @Test
    void testArgumentWithEmptyParameterName() {
        CLIArgument arg = new CLIArgument("-", '=', "-=value");
        assertEquals("-", arg.getPrefix());
        assertEquals("", arg.getParameterName());
        assertEquals("value", arg.getValue());
    }

    @Test
    void testArgumentWithOnlyPrefix() {
        CLIArgument arg = new CLIArgument("-", '=', "-");
        assertEquals("-", arg.getPrefix());
        assertEquals("", arg.getParameterName());
        assertNull(arg.getValue());
    }

    @Test
    void testArgumentWithSpecialCharactersInValue() {
        CLIArgument arg = new CLIArgument("-", '=', "-token=abc!@#$%^&*()");
        assertEquals("token", arg.getParameterName());
        assertEquals("abc!@#$%^&*()", arg.getValue());
    }

    @Test
    void testArgumentWithUnicode() {
        CLIArgument arg = new CLIArgument("-", '=', "-lang=日本語");
        assertEquals("lang", arg.getParameterName());
        assertEquals("日本語", arg.getValue());
    }

    @Test
    void testNullArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CLIArgument("-", '=', null));
    }

    @Test
    void testNullPrefixThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CLIArgument(null, '=', "-port=8080"));
    }

}
