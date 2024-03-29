package de.morihofi.acmeserver.certificate.acme.challenges;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.crypto.AcmeTokenCryptography;
import de.morihofi.acmeserver.tools.crypto.AcmeUtils;
import de.morihofi.acmeserver.tools.regex.DomainAndIpValidation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class HTTPChallenge {

    private HTTPChallenge() {
    }

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(HTTPChallenge.class);
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().proxy(getHTTPChallengeProxy()).build();

    /**
     * User Agent used for checking HTTP challenges
     */
    private static final String USER_AGENT = "Mozilla/5.0 ACMEServer/" + Main.buildMetadataVersion + " Java/" + System.getProperty("java.version");

    private static String proxyHost = "";
    private static int proxyPort = 0;
    private static String proxyUser = "";
    private static String proxyPassword = "";


    /**
     * Retrieves and configures an HTTP challenge proxy based on application settings.
     * The method configures the proxy settings and authentication details, if required.
     *
     * @return A Proxy object configured based on application settings.
     */
    public static Proxy getHTTPChallengeProxy() {
        Proxy.Type proxyType = switch (Main.appConfig.getProxy().getHttpChallenge().getType()) {
            case "socks" -> Proxy.Type.SOCKS;
            case "http" -> Proxy.Type.HTTP;
            default -> Proxy.Type.DIRECT;
        };
        Proxy proxy = Proxy.NO_PROXY;

        try {
            proxyPort = Main.appConfig.getProxy().getHttpChallenge().getPort();
            proxyHost = Main.appConfig.getProxy().getHttpChallenge().getHost();


            if (Main.appConfig.getProxy().getHttpChallenge().getEnabled()) {
                SocketAddress socketAddress = new InetSocketAddress(proxyHost, proxyPort);
                proxy = new Proxy(proxyType, socketAddress);
            }


            if (Main.appConfig.getProxy().getHttpChallenge().getAuthentication().isEnabled()) {
                proxyUser = Main.appConfig.getProxy().getHttpChallenge().getAuthentication().getUsername();
                proxyPassword = Main.appConfig.getProxy().getHttpChallenge().getAuthentication().getPassword();

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(proxyHost) && proxyPort == getRequestingPort()) {
                            return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                        }
                        return null;
                    }
                });

            }


        } catch (Exception ex) {
            log.error("Failed to initialize proxy configuration", ex);
        }


        return proxy;
    }


    /**
     * Validates an HTTP challenge by sending a GET request to the specified host and verifying the response.
     * The method checks whether the response body contains the expected token, which indicates successful validation.
     *
     * @param authToken   The expected authentication token value for the challenge.
     * @param host        The target host for the HTTP GET request.
     * @param acmeAccount The ACME account used in the challenge.
     * @return {@code true} if the challenge validation is successful, otherwise {@code false}.
     * @throws IOException              If an I/O error occurs during the HTTP request.
     * @throws NoSuchAlgorithmException If a requested cryptographic algorithm is not available.
     * @throws InvalidKeySpecException  If an invalid key specification is encountered.
     * @throws NoSuchProviderException  If a requested security provider is not available.
     */
    public static ChallengeResult check(String authToken, String host, ACMEAccount acmeAccount) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        boolean passed = false;
        String lastError = "";

        PublicKey acmeAccountPublicKey = PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM());

        // Host can be an IP Address, specifically an IPv6 Address. This type of IP Address needs these "[ ]" square brackets when you use it in a URL
        // Let's check that
        if (DomainAndIpValidation.isIPv6Address(host)) {
            host = "[" + host + "]"; // That's it
        }

        try {
            // Create an HTTP GET request to the challenge URL
            Request request = new Request.Builder()
                    .url("http://" + host + "/.well-known/acme-challenge/" + authToken)
                    .header("User-Agent", USER_AGENT)
                    .build();

            if (log.isDebugEnabled()) {
                log.debug("Performing GET request to \"{}\"", request.url());
            }

            // Execute the HTTP GET request and retrieve the response
            try (Response response = httpClient.newCall(request).execute()) {
                int responseCode = response.code();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Successful response, check the token in the response
                    log.debug("Got response, checking token in response.");
                    assert response.body() != null;
                    String acmeTokenFromHost = response.body().string();
                    String expectedValue = getToken(authToken, acmeAccountPublicKey);

                    if (expectedValue.equals(acmeTokenFromHost)) {
                        passed = true;
                        log.info("HTTP Challenge has validated for host {}. Expected: {}; Got: {}", host, expectedValue, acmeTokenFromHost);
                    } else {
                        log.error("HTTP Challenge validation failed for host {}. Content doesn't match. Expected: {}; Got: {}", host, expectedValue, acmeTokenFromHost);
                        lastError = "HTTP Challenge validation failed, cause content doesn't match";
                    }
                } else {
                    log.error("HTTP Challenge failed for host {}, got HTTP status code {}", host, responseCode);
                    lastError = "HTTP Challenge failed, got HTTP status code " + responseCode;
                }
            }


        } catch (IOException e) {
            log.error("HTTP Challenge failed for host {}. Is it reachable?", host, e);
            if(e instanceof ConnectException){
                lastError = e.getMessage();
            }
        }


        return new ChallengeResult(passed, lastError);
    }

    private static String getToken(String authToken, PublicKey acmeAccountPublicKey) {

        if (!AcmeUtils.isValidBase64Url(authToken)) {
            throw new IllegalArgumentException("Invalid auth token: " + authToken);
        }
        return AcmeTokenCryptography.keyAuthorizationFor(authToken, acmeAccountPublicKey);
    }


}
