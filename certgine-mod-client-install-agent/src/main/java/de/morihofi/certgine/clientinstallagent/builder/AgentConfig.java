package de.morihofi.certgine.clientinstallagent.builder;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class AgentConfig {
    @Builder.Default
    private final int version = 1;
    private Client client;
    private Service service;
    private CertificateAuthority certificate_authority;
    private Security security;

    @Data
    @Builder
    public static class Client {
        private String type;
        private boolean enforce_elevated;
        private boolean unattended_mode;
        private boolean silent_mode;
        private String title;
    }

    @Data
    @Builder
    public static class Service {
        private String host;
        private int port;
        private boolean use_tls;
        private int timeout;
        private int retry_attempts;
    }

    @Data
    @Builder
    public static class CertificateAuthority {
        private String source;
        private List<String> trust_anchors;
    }

    @Data
    @Builder
    public static class Security {
        private List<String> allowed_roots;
    }
}
