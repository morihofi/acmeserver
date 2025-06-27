package de.morihofi.certgine.tsa;

import de.morihofi.certgine.server.common.intf.ServletMount;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import de.morihofi.certgine.cryptography.tsa.TimeStampAuthority;
import org.bouncycastle.tsp.TimeStampRequest;

import java.io.IOException;
import java.io.InputStream;

/**
 * Servlet providing RFC 3161 timestamping service.
 */
@Slf4j
@ServletMount(servletMountPoint = "/tsa", protect = true)
public class TimeStampServlet extends HttpServlet {
    private final TimeStampAuthority authority;

    public TimeStampServlet(TimeStampAuthority authority) {
        this.authority = authority;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"application/timestamp-query".equals(req.getContentType())) {
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        byte[] data;
        try (InputStream in = req.getInputStream()) {
            data = in.readAllBytes();
        }
        TimeStampRequest tsReq;
        try {
            tsReq = new TimeStampRequest(data);
        } catch (Exception e) {
            log.warn("Malformed timestamp request", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            byte[] encoded = authority.generate(tsReq);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/timestamp-reply");
            resp.getOutputStream().write(encoded);
        } catch (Exception e) {
            log.error("Failed to generate timestamp", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
