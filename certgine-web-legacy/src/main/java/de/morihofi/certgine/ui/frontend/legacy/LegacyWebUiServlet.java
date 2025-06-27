package de.morihofi.certgine.ui.frontend.legacy;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.authority.EcdsaCertificateAlgorithm;
import de.morihofi.certgine.types.database.entities.authority.RsaCertificateAlgorithm;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.ui.frontend.legacy.type.CaEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
@ServletMount(servletMountPoint = "/legacy/*", protect = true)
public class LegacyWebUiServlet extends AbstractJteRouterServlet {

    private final IServerInstance serverInstance;

    public LegacyWebUiServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    @Override
    protected void registerRoutes(Map<String, BiConsumer<HttpServletRequest, HttpServletResponse>> routes) {
        routes.put("/", this::handleIndex);
        routes.put("/about", this::handleAbout);
        routes.put("/help", this::handleHelp);
        routes.put("/ca-list", this::handleCaList);
        routes.put("/ca", this::handleCaDetails);
    }

    @Override
    protected String getBaseUrl(HttpServletRequest request) {
        return serverInstance.getServerURL(); // z. B. https://example.com
    }

    private void handleIndex(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> params = getBaseParams(req);
            params.put("cas", getRootCertList());
            render(resp, "legacy/index.jte", params);
        } catch (IOException e) {
            log.error("Error rendering index", e);
        }
    }

    private void handleAbout(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> params = getBaseParams(req);
            render(resp, "legacy/about.jte", params);
        } catch (IOException e) {
            log.error("Error rendering about page", e);
        }
    }

    private void handleHelp(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> params = getBaseParams(req);
            render(resp, "legacy/help.jte", params);
        } catch (IOException e) {
            log.error("Error rendering help page", e);
        }
    }

    private void handleCaList(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> params = getBaseParams(req);
            params.put("cas", getRootCertList());
            render(resp, "legacy/ca-list.jte", params);
        } catch (IOException e) {
            log.error("Error rendering CA list", e);
        }
    }

    private void handleCaDetails(HttpServletRequest req, HttpServletResponse resp) {
        String id = req.getParameter("id");
        if (id == null || id.isEmpty()) {
            try {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException ignored) {
            }
            return;
        }
        try {
            RootCa ca = RootCa.getForUuid(serverInstance, id);
            if (ca == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            CaEntry entry = new CaEntry();
            entry.setId(ca.getInternalUuid());
            entry.setName(ca.getCertificateConfig().getMetadata().getCommonName());
            entry.setDescription("Root Certificate");
            entry.setPemPath("/dl/rootca/" + ca.getInternalUuid() + ".pem");
            entry.setDerPath("/dl/rootca/" + ca.getInternalUuid() + ".der");
            entry.setCabPath("/dl/rootca/" + ca.getInternalUuid() + ".cab");
            entry.setEcdsa(ca.getCertificateConfig().getCertificateAlgorithm() instanceof EcdsaCertificateAlgorithm);
            entry.setCommonName(ca.getCertificateConfig().getMetadata().getCommonName());
            entry.setOrganisation(ca.getCertificateConfig().getMetadata().getOrganisation());
            entry.setOrganisationalUnit(ca.getCertificateConfig().getMetadata().getOrganisationalUnit());
            entry.setCountryCode(ca.getCertificateConfig().getMetadata().getCountryCode());

            if (ca.getCertificateConfig().getCertificateAlgorithm() instanceof RsaCertificateAlgorithm rsaAlg) {
                entry.setAlgorithmDetail("RSA " + rsaAlg.getKeySize() + " bit");
            } else if (ca.getCertificateConfig().getCertificateAlgorithm() instanceof EcdsaCertificateAlgorithm ecdsaAlg) {
                entry.setAlgorithmDetail("ECDSA " + ecdsaAlg.getCurveName());
            }

            try {
                java.security.cert.X509Certificate cert = serverInstance.getCryptoStoreManager().getCertficateAuthorityX509Certificate(ca);
                entry.setSha1Fingerprint(de.morihofi.certgine.ui.frontend.legacy.util.CertificateUtil.getFingerprint(cert, "SHA-1"));
                entry.setSha256Fingerprint(de.morihofi.certgine.ui.frontend.legacy.util.CertificateUtil.getFingerprint(cert, "SHA-256"));
            } catch (Exception ex) {
                log.error("Could not read certificate", ex);
            }

            Map<String, Object> params = getBaseParams(req);
            params.put("ca", entry);
            render(resp, "legacy/ca-details.jte", params);
        } catch (IOException e) {
            log.error("Error rendering CA details", e);
        }
    }

    private List<CaEntry> getRootCertList() {
        List<CaEntry> caList = new ArrayList<>();
        try (Session s = serverInstance.getDatabaseSession()) {
            RootCa[] allRoots = RootCa.getAllRoots(s);
            for (RootCa rootCa : allRoots) {
                CaEntry entry = new CaEntry();
                entry.setId(rootCa.getInternalUuid());
                entry.setName(rootCa.getCertificateConfig().getMetadata().getCommonName());
                entry.setDescription("Root Certificate");
                entry.setPrimary(false); //TODO: Implement logic to determine if this is the primary CA
                entry.setPemPath("/dl/rootca/" + rootCa.getInternalUuid() + ".pem");
                entry.setDerPath("/dl/rootca/" + rootCa.getInternalUuid() + ".der");
                entry.setCabPath("/dl/rootca/" + rootCa.getInternalUuid() + ".cab");
                entry.setEcdsa(rootCa.getCertificateConfig().getCertificateAlgorithm() instanceof EcdsaCertificateAlgorithm);
                caList.add(entry);
            }
        }
        return caList;
    }

    private Map<String, Object> getBaseParams(HttpServletRequest req) {
        String userAgent = Optional.ofNullable(req.getHeader("User-Agent")).orElse("").toLowerCase();
        Map<String, Object> params = new HashMap<>();
        params.put("baseUrl", getBaseUrl(req));
        params.put("isWindowsMobile", userAgent.contains("windows ce") || userAgent.contains("iemobile") || userAgent.contains("msie 4"));
        params.put("isWindowsDesktop", userAgent.contains("windows nt"));
        params.put("isAndroid", userAgent.contains("android"));
        return params;
    }
}
