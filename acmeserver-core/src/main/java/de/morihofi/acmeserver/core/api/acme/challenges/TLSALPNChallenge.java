package de.morihofi.acmeserver.core.api.acme.challenges;

import de.morihofi.acmeserver.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1OctetString;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Utilities for validating TLS-ALPN-01 challenges.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TLSALPNChallenge {
    private static final String ACME_ID_OID = "1.3.6.1.5.5.7.1.31";
    private static final String PROTO = "acme-tls/1";

    /**
     * Validates the TLS-ALPN-01 challenge for the given host.
     *
     * @param token          Challenge token
     * @param host           Hostname or IP address
     * @param account        ACME account
     * @param serverInstance server instance
     * @return result of the challenge validation
     */
    @NonNull
    public static ChallengeResult check(@NonNull String token,
                                        @NonNull String host,
                                        @NonNull AcmeAccount account,
                                        @NonNull IServerInstance serverInstance) throws IOException, GeneralSecurityException, InvalidKeySpecException {
        String error = "";
        boolean success = false;

        PublicKey accountKey = PemUtil.readPublicKeyFromPem(account.getPublicKeyPEM());
        String keyAuth = AcmeTokenCryptography.keyAuthorizationFor(token, accountKey);
        byte[] expected = MessageDigest.getInstance("SHA-256").digest(keyAuth.getBytes());

        int port = 443;
        String hostname = host;
        if (host.startsWith("[") && host.contains("]")) {
            int end = host.indexOf(']');
            String ip = host.substring(1, end);
            if (host.length() > end + 1 && host.charAt(end + 1) == ':') {
                port = Integer.parseInt(host.substring(end + 2));
            }
            hostname = ip;
        } else if (host.contains(":")) {
            int idx = host.lastIndexOf(':');
            if (host.indexOf(':') == idx) {
                port = Integer.parseInt(host.substring(idx + 1));
                hostname = host.substring(0, idx);
            }
        }

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, null);

        SSLSocketFactory factory = ctx.getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
            SSLParameters params = socket.getSSLParameters();
            params.setApplicationProtocols(new String[]{PROTO});
            socket.setSSLParameters(params);
            socket.connect(new InetSocketAddress(hostname, port), 5000);
            socket.startHandshake();
            SSLSession session = socket.getSession();
            if (!PROTO.equals(socket.getApplicationProtocol())) {
                error = "ALPN protocol mismatch";
            } else {
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                byte[] ext = cert.getExtensionValue(ACME_ID_OID);
                if (ext != null) {
                    byte[] digest = ASN1OctetString.getInstance(ext).getOctets();
                    if (Arrays.equals(expected, digest)) {
                        success = true;
                    } else {
                        error = "acmeIdentifier digest mismatch";
                    }
                } else {
                    error = "acmeIdentifier extension missing";
                }
            }
        } catch (Exception e) {
            log.error("TLS-ALPN challenge failed", e);
            error = e.getMessage();
        }

        return new ChallengeResult(success, error);
    }
}

