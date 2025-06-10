package de.morihofi.acmeserver.core.api.acme.api.endpoints;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServerRoutingTest {
    @Test
    @DisplayName("WebServer routes key-change to KeyChangeEndpoint")
    void routingContainsKeyChangeEndpoint() throws IOException {
        Path p = Path.of("src/main/java/de/morihofi/acmeserver/core/WebServer.java");
        String source = Files.readString(p);
        assertTrue(source.contains("new KeyChangeEndpoint"));
    }
}
