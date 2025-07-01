package de.morihofi.certgine.server.common.intf;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.proxy.ProxyServlet;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

@Slf4j
public abstract class ReverseProxyServletHolder extends ServletHolder {

    private final String targetBaseUri;
    private final String prefix;

    /**
     * @param targetBaseUri z.B. "http://localhost:5173"
     */
    protected ReverseProxyServletHolder(String targetBaseUri, String prefix) {
        super(new ProxyServlet.Transparent());

        this.targetBaseUri = targetBaseUri.endsWith("/")
                ? targetBaseUri.substring(0, targetBaseUri.length() - 1)
                : targetBaseUri;
        this.prefix = prefix;

        this.setInitParameter("proxyTo", targetBaseUri);
        this.setInitParameter("prefix", prefix);
        this.setInitParameter("idleTimeout", "60000");   // optional

    }

}
