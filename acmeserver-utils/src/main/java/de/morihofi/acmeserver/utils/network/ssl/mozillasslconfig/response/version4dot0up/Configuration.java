/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.response.version4dot0up;

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
