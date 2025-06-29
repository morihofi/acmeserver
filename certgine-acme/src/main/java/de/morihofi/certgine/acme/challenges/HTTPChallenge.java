/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.challenges;

import de.morihofi.certgine.types.database.entities.acme.AcmeAccount;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.cryptography.acme.AcmeTokenCryptography;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.crypto.AcmeUtils;
import de.morihofi.certgine.utils.regex.IpValidator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HTTPChallenge {

    /**
     * Validates an HTTP challenge by sending a GET request to the specified host and verifying the response. The method checks whether the
     * response body contains the expected token, which indicates successful validation.
     *
     * @param authToken      The expected authentication token value for the challenge.
     * @param host           The target host for the HTTP GET request.
     * @param acmeAccount    The ACME account used in the challenge.
     * @param serverInstance Server instance
     * @return {@code true} if the challenge validation is successful, otherwise {@code false}.
     * @throws IOException              If an I/O error occurs during the HTTP request.
     * @throws NoSuchAlgorithmException If a requested cryptographic algorithm is not available.
     * @throws InvalidKeySpecException  If an invalid key specification is encountered.
     * @throws NoSuchProviderException  If a requested security provider is not available.
     */
    @NonNull
    public static ChallengeResult check(@NonNull String authToken, @NonNull String host, @NonNull AcmeAccount acmeAccount, @NonNull IServerInstance serverInstance) throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        boolean passed = false;
        String lastError = "";

        OkHttpClient httpClient = serverInstance.getNetworkClient().getOkHttpClient();

        PublicKey acmeAccountPublicKey = PemUtil.readPublicKeyFromPem(acmeAccount.getPublicKeyPEM());

        // Host can be an IP Address, specifically an IPv6 Address. This type of IP Address needs these "[ ]" square brackets when you
        // use it in a URL
        // Let's check that
        if (IpValidator.isIPv6Address(host)) {
            host = "[" + host + "]"; // That's it
        }

        try {
            // Create an HTTP GET request to the challenge URL
            Request request = new Request.Builder()
                    .url("http://" + host + "/.well-known/acme-challenge/" + authToken)
                    .header("User-Agent", "Mozilla/5.0 ACMEServer/" + serverInstance.getBuildMetadata().getBuildVersion() + "+git" + serverInstance.getBuildMetadata().getGitCommit() + " Java/" + System.getProperty("java.version"))
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
                        log.error("HTTP Challenge validation failed for host {}. Content doesn't match. Expected: {}; Got: {}", host,
                                expectedValue, acmeTokenFromHost);
                        lastError = "HTTP Challenge validation failed, cause content doesn't match";
                    }
                } else {
                    log.error("HTTP Challenge failed for host {}, got HTTP status code {}", host, responseCode);
                    lastError = "HTTP Challenge failed, got HTTP status code " + responseCode;
                }
            }
        } catch (IOException e) {
            log.error("HTTP Challenge failed for host {}. Is it reachable?", host, e);
            if (e instanceof ConnectException) {
                lastError = e.getMessage();
            }
        }

        return new ChallengeResult(passed, lastError);
    }

    @NonNull
    private static String getToken(@NonNull String authToken, @NonNull PublicKey acmeAccountPublicKey) {

        if (!AcmeUtils.isValidBase64Url(authToken)) {
            throw new IllegalArgumentException("Invalid auth token: " + authToken);
        }
        return AcmeTokenCryptography.keyAuthorizationFor(authToken, acmeAccountPublicKey);
    }
}
