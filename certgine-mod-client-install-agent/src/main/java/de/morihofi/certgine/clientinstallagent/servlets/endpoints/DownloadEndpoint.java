package de.morihofi.certgine.clientinstallagent.servlets.endpoints;

import de.morihofi.certgine.clientinstallagent.ClientInstallAgentModuleInstance;
import de.morihofi.certgine.clientinstallagent.builder.AgentConfig;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DownloadEndpoint implements Handler {

    private final ClientInstallAgentModuleInstance moduleInstance;

    @Override
    public void handle(HandlerContext context) throws Exception {

        IServerInstance serverInstance = moduleInstance.getModule().getServerInstance();

        List<String> install_cas = Arrays.stream(RootCa.getAllRoots(serverInstance.getDatabaseSession()))
                .map(RootCa::getInternalUuid).collect(Collectors.toSet()).stream().toList();

        List<String> trust_cas = List.of(getCurrentRootCaBase64(serverInstance));

        AgentConfig config = AgentConfig.builder()
                .client(AgentConfig.Client.builder()
                        .type("gui")
                        .enforce_elevated(true)
                        .unattended_mode(false)
                        .silent_mode(false)
                        .build())
                .service(AgentConfig.Service.builder()
                        .host(serverInstance.getAppConfig().getServer().getDnsName())
                        .port(serverInstance.getAppConfig().getServer().getPorts().getHttps())
                        .use_tls(true)
                        .timeout(30)
                        .retry_attempts(3)
                        .build())
                .certificate_authority(AgentConfig.CertificateAuthority.builder()
                        .source("remote")
                        .trust_anchors(install_cas)
                        .build())
                .security(AgentConfig.Security.builder()
                        .allowed_roots(trust_cas)
                        .build())
                .build();

        ByteBuffer bb = moduleInstance.getAgentGenerator().generateAgent(config);

        context.header("Content-Disposition", "attachment; filename=\"certgine_agent.exe\"\n");
        context.result(bb.array());
    }

    /**
     * Get the current used root ca as Base64 URL
     * @param serverInstance
     * @return
     * @throws KeyStoreException
     * @throws CertificateEncodingException
     */
    private String getCurrentRootCaBase64(IServerInstance serverInstance) throws KeyStoreException, CertificateEncodingException {

        byte[] encodedRoot = serverInstance.getCryptoStoreManager()
                .getCertificateAuthorityX509Certificate(serverInstance.getRootCa())
                .getEncoded();

        return Base64.getUrlEncoder().encodeToString(encodedRoot);
    }
}
