/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.challenges;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.ServerInstance;
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
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class HTTPChallenge {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    /**
     * User Agent used for checking HTTP challenges
     */
    private static final String USER_AGENT =
            "Mozilla/5.0 ACMEServer/" + Main.buildMetadataVersion + "+git" + Main.buildMetadataGitCommit + " Java/" + System.getProperty(
                    "java.version");
    private static String proxyHost = "";
    private static int proxyPort = 0;
    private static String proxyUser = "";
    private static String proxyPassword = "";



    /**
     * Validates an HTTP challenge by sending a GET request to the specified host and verifying the response. The method checks whether the
     * response body contains the expected token, which indicates successful validation.
     *
     * @param authToken   The expected authentication token value for the challenge.
     * @param host        The target host for the HTTP GET request.
     * @param acmeAccount The ACME account used in the challenge.
     * @param serverInstance Server instance
     * @return {@code true} if the challenge validation is successful, otherwise {@code false}.
     * @throws IOException              If an I/O error occurs during the HTTP request.
     * @throws NoSuchAlgorithmException If a requested cryptographic algorithm is not available.
     * @throws InvalidKeySpecException  If an invalid key specification is encountered.
     * @throws NoSuchProviderException  If a requested security provider is not available.
     */
    public static ChallengeResult check(String authToken, String host, ACMEAccount acmeAccount, ServerInstance serverInstance) throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        boolean passed = false;
        String lastError = "";

        OkHttpClient httpClient = serverInstance.getNetworkClient().getOkHttpClient();


        PublicKey acmeAccountPublicKey = PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM());

        // Host can be an IP Address, specifically an IPv6 Address. This type of IP Address needs these "[ ]" square brackets when you
        // use it in a URL
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

            if (LOG.isDebugEnabled()) {
                LOG.debug("Performing GET request to \"{}\"", request.url());
            }

            // Execute the HTTP GET request and retrieve the response
            try (Response response = httpClient.newCall(request).execute()) {
                int responseCode = response.code();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Successful response, check the token in the response
                    LOG.debug("Got response, checking token in response.");
                    assert response.body() != null;
                    String acmeTokenFromHost = response.body().string();
                    String expectedValue = getToken(authToken, acmeAccountPublicKey);

                    if (expectedValue.equals(acmeTokenFromHost)) {
                        passed = true;
                        LOG.info("HTTP Challenge has validated for host {}. Expected: {}; Got: {}", host, expectedValue, acmeTokenFromHost);
                    } else {
                        LOG.error("HTTP Challenge validation failed for host {}. Content doesn't match. Expected: {}; Got: {}", host,
                                expectedValue, acmeTokenFromHost);
                        lastError = "HTTP Challenge validation failed, cause content doesn't match";
                    }
                } else {
                    LOG.error("HTTP Challenge failed for host {}, got HTTP status code {}", host, responseCode);
                    lastError = "HTTP Challenge failed, got HTTP status code " + responseCode;
                }
            }
        } catch (IOException e) {
            LOG.error("HTTP Challenge failed for host {}. Is it reachable?", host, e);
            if (e instanceof ConnectException) {
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

    private HTTPChallenge() {
    }
}
