/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.tsa.servlets;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.tsa.types.entities.TsaAuthority;
import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import de.morihofi.certgine.cryptography.tsa.TimeStampAuthority;
import org.bouncycastle.tsp.TimeStampRequest;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.List;

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

    /**
     * Creates a servlet instance using the given server instance and the first registered TSA authority.
     *
     * @param serverInstance running server instance
     */
    public TimeStampServlet(IServerInstance serverInstance) throws UnrecoverableKeyException,
            KeyStoreException, NoSuchAlgorithmException {
        this(serverInstance, TsaAuthority.getAll(serverInstance)[0]);
    }

    /**
     * Creates a new servlet instance using the given server instance.
     * <p>
     * The {@link TimeStampAuthority} is constructed using the TSA key pair and
     * certificate available from the provided {@link IServerInstance}.
     *
     * @param serverInstance running server instance
     */
    public TimeStampServlet(IServerInstance serverInstance, TsaAuthority authority) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        X509Certificate tsaCert = serverInstance
                .getCryptoStoreManager()
                .getTimestampAuthorityCertificate(authority.getInternalUuid());
        this.authority = new TimeStampAuthority(
                serverInstance.getCryptoStoreManager()
                        .getTimestampAuthorityKeyPair(authority.getInternalUuid())
                        .getPrivate(),
                tsaCert,
                List.of(tsaCert,
                        serverInstance.getCryptoStoreManager()
                                .getCertificateAuthorityX509Certificate(serverInstance.getRootCa())));
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
