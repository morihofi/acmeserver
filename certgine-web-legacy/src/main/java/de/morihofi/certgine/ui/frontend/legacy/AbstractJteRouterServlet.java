package de.morihofi.certgine.ui.frontend.legacy;

import de.morihofi.certgine.ui.frontend.legacy.helper.TemplateHelper;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Router-based legacy servlet base class with static handler.
 */
@Slf4j
public abstract class AbstractJteRouterServlet extends HttpServlet {

    private final TemplateEngine templateEngine;

    public AbstractJteRouterServlet() {
        this.templateEngine = TemplateHelper.createTemplateEngine();
    }

    protected abstract void registerRoutes(Map<String, BiConsumer<HttpServletRequest, HttpServletResponse>> routes);

    protected abstract String getBaseUrl(HttpServletRequest request);

    @Override
    public void init() throws ServletException {
        if (!resourceDirectoryExists()) {
            throw new ServletException("Static resource directory /static not found in classpath.");
        }
    }

    private boolean resourceDirectoryExists() {
        URL resource = getClass().getResource("/static/");
        return resource != null;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (path.startsWith("/static/")) {
            serveStatic(path, resp);
            return;
        }

        Map<String, BiConsumer<HttpServletRequest, HttpServletResponse>> routes = new HashMap<>();
        registerRoutes(routes);

        BiConsumer<HttpServletRequest, HttpServletResponse> handler = routes.get(path);
        if (handler != null) {
            handler.accept(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void serveStatic(String pathInfo, HttpServletResponse resp) throws IOException {
        String resourcePath = "/static" + pathInfo.substring("/static".length());
        URL resource = getClass().getResource(resourcePath);
        if (resource == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = Files.probeContentType(new File(pathInfo).toPath());
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }

        resp.setStatus(HttpServletResponse.SC_OK);

        try (InputStream in = resource.openStream()) {
            in.transferTo(resp.getOutputStream());
        }
    }

    protected void render(HttpServletResponse resp, String template, Map<String, Object> params) throws IOException {
        StringOutput output = new StringOutput();
        templateEngine.render(template, params, output);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(output.toString());
    }
}
