package de.morihofi.certgine.clientinstallagent.builder;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AgentConfigSerializationTest {

    @Test
    void clientSilentModeAndTitlesSerialized() {
        AgentConfig config = AgentConfig.builder()
                .client(AgentConfig.Client.builder()
                        .type("gui")
                        .enforce_elevated(false)
                        .unattended_mode(false)
                        .silent_mode(true)
                        .title("Custom")
                        .build())
                .service(AgentConfig.Service.builder()
                        .host("localhost")
                        .port(443)
                        .use_tls(true)
                        .timeout(30)
                        .retry_attempts(3)
                        .build())
                .certificate_authority(AgentConfig.CertificateAuthority.builder()
                        .source("remote")
                        .trust_anchors(List.of("abc"))
                        .build())
                .security(AgentConfig.Security.builder()
                        .allowed_roots(List.of("def"))
                        .build())
                .build();

        String json = new Gson().toJson(config);
        assertTrue(json.contains("\"silent_mode\":true"));
        assertTrue(json.contains("\"title\":\"Custom\""));
        assertTrue(json.contains("\"help_link\":\"https://example.com\""));
    }
}
