/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version4dot0up;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

/**
 * Represents the SSL/TLS configuration settings for various levels as defined by Mozilla's SSL Configuration.
 * This class includes details like ECDH parameters, supported TLS versions, cipher suites, and other cryptographic settings.
 */
@Getter
public class Configuration {

    /**
     * Size of the ECDH parameter.
     */
    @SerializedName("ecdh_param_size")
    private long ecdhParamSize;

    /**
     * List of the oldest supported clients.
     */
    @SerializedName("oldest_clients")
    private List<String> oldestClients;

    /**
     * List of supported TLS versions.
     */
    @SerializedName("tls_versions")
    private List<String> tlsVersions;

    /**
     * Size of the DH parameter.
     */
    @SerializedName("dh_param_size")
    private Long dhParamSize;

    /**
     * OpenSSL cipher suites configuration string.
     */
    @SerializedName("openssl_ciphersuites")
    private String opensslCiphersuites;

    /**
     * Minimum age for HSTS (HTTP Strict Transport Security) in seconds.
     */
    @SerializedName("hsts_min_age")
    private long hstsMinAge;

    /**
     * List of supported certificate signatures.
     */
    @SerializedName("certificate_signatures")
    private List<String> certificateSignatures;

    /**
     * List of supported TLS curves.
     */
    @SerializedName("tls_curves")
    private List<String> tlsCurves;

    /**
     * List of supported cipher suites.
     */
    @SerializedName("ciphersuites")
    private List<String> ciphersuites;

    /**
     * List of supported certificate types.
     */
    @SerializedName("certificate_types")
    private List<String> certificateTypes;

    /**
     * Size of the RSA key.
     */
    @SerializedName("rsa_key_size")
    private long rsaKeySize;

    /**
     * List of supported certificate curves.
     */
    @SerializedName("certificate_curves")
    private List<String> certificateCurves;
}
