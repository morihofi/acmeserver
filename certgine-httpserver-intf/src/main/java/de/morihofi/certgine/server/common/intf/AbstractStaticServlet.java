package de.morihofi.certgine.server.common.intf;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

public abstract class AbstractStaticServlet extends HttpServlet {

    protected abstract String getBasePath(); // e.g., "/webapp-modern"
    public abstract String getMountPath();   // e.g., "/modern/*"

    @Override
    public void init() throws ServletException {
        super.init();
        // Check base path exists at init
        if (!resourceDirectoryExists()) {
            throw new ServletException("Base classpath directory " + getBasePath() + " not found in classpath for " + getClass().getName());
        }
    }

    private boolean resourceDirectoryExists() {
        URL resource = getClass().getResource(getBasePath() + "/");
        return resource != null;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!Objects.equals(req.getMethod(), "GET") && !Objects.equals(req.getMethod(), "HEAD")) {
            super.service(req, resp); // Delegate other methods
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            pathInfo = "/index.html";
        }

        String fullPath = getBasePath() + pathInfo;
        URL resource = getClass().getResource(fullPath);

        if (resource == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = Files.probeContentType(new File(pathInfo).toPath());
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }
        resp.setStatus(HttpServletResponse.SC_OK);

        if ("GET".equals(req.getMethod())) {
            try (InputStream in = resource.openStream()) {
                in.transferTo(resp.getOutputStream());
            }
        }
        // HEAD: no body output
    }
}
