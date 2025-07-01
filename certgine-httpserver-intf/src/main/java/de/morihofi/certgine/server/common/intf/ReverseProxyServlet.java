package de.morihofi.certgine.server.common.intf;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public abstract class ReverseProxyServlet extends HttpServlet {

    private final String targetBaseUri;
    private HttpClient client;


    protected ReverseProxyServlet(String targetBaseUri) {
        this.targetBaseUri = targetBaseUri;
    }

    @Override
    public void init() {
        log.info("Initializing ReverseProxyServlet for target: {}", targetBaseUri);
        this.client = new HttpClient();
        try {
            client.start();
            log.info("HttpClient started successfully");
        } catch (Exception e) {
            log.error("Failed to start HttpClient", e);
            throw new RuntimeException("HttpClient startup failure", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String rewrittenUri = targetBaseUri + req.getRequestURI();
        log.debug("Proxying to: {}", rewrittenUri);

        try {
            ContentResponse response = client.newRequest(rewrittenUri).method(req.getMethod()).headers(h -> {
                Collections.list(req.getHeaderNames()).forEach(name -> h.put(name, req.getHeader(name)));
            }).send();

            resp.setStatus(response.getStatus());
            response.getHeaders().forEach(field -> resp.addHeader(field.getName(), field.getValue()));

            resp.getOutputStream().write(response.getContent());
        } catch (Exception e) {
            log.error("Proxy request failed", e);
            resp.setStatus(502);
            resp.getWriter().write("Proxy error");
        }
    }

    @Override
    public void destroy() {
        try {
            client.stop();
            log.info("HttpClient stopped");
        } catch (Exception e) {
            log.warn("Failed to stop HttpClient cleanly", e);
        }
    }
}

