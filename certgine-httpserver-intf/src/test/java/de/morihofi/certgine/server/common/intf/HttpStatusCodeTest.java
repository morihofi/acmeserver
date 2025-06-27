package de.morihofi.certgine.server.common.intf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpStatusCodeTest {
    @Test
    @DisplayName("message from code returns known message")
    void testKnown(){
        assertEquals("OK", HttpStatusCode.getMessageFromCode(200));
    }

    @Test
    @DisplayName("unknown code returns default")
    void testUnknown(){
        assertEquals("Unknown", HttpStatusCode.getMessageFromCode(9999));
    }
}
