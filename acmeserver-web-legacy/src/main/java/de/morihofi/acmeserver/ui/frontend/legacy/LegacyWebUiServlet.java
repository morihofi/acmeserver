package de.morihofi.acmeserver.ui.frontend.legacy;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.ui.frontend.legacy.type.CaEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class LegacyWebUiServlet extends AbstractJteRouterServlet {

    public static final String PATH_MOUNT = "/legacy/*";
    private final IServerInstance serverInstance;

    public LegacyWebUiServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    @Override
    protected void registerRoutes(Map<String, BiConsumer<HttpServletRequest, HttpServletResponse>> routes) {
        routes.put("/", this::handleIndex);
        routes.put("/about", this::handleAbout);
        routes.put("/ca-list", this::handleCaList);
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

    private void handleCaList(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> params = getBaseParams(req);
            params.put("cas", getRootCertList());
            render(resp, "legacy/ca-list.jte", params);
        } catch (IOException e) {
            log.error("Error rendering CA list", e);
        }
    }

    private List<CaEntry> getRootCertList() {
        List<CaEntry> caList = new ArrayList<>();
        try (Session s = serverInstance.getDatabaseSession()) {
            RootCa[] allRoots = RootCa.getAllRoots(s);
            for (RootCa rootCa : allRoots) {
                CaEntry entry = new CaEntry();
                entry.setName(rootCa.getInternalUuid());
                entry.setDescription("Placeholder description"); // z.B. rootCa.getCommonName()
                entry.setPrimary(false); //TODO: Implement logic to determine if this is the primary CA
                entry.setPemPath("/dl/certs/root/" + rootCa.getInternalUuid() + ".pem");
                entry.setDerPath("/dl/certs/root/" + rootCa.getInternalUuid() + ".der");
                entry.setCabPath("/dl/certs/root/" + rootCa.getInternalUuid() + ".cab");
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
