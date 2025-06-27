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

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version5dot1up;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

/**
 * Represents the SSL/TLS configuration settings.
 */
@Getter
public class Configuration {

    /**
     * The size of the ECDH parameter.
     */
    @SerializedName("ecdh_param_size")
    private long ecdhParamSize;

    /**
     * The oldest clients supported by the configuration.
     */
    @SerializedName("oldest_clients")
    private List<String> oldestClients;

    /**
     * Indicates if the server prefers its own order of cipher suites.
     */
    @SerializedName("server_preferred_order")
    private boolean serverPreferredOrder;

    /**
     * The list of cipher suites.
     */
    @SerializedName("ciphersuites")
    private List<String> ciphersuites;

    /**
     * The maximum certificate lifespan.
     */
    @SerializedName("maximum_certificate_lifespan")
    private long maximumCertificateLifespan;

    /**
     * The recommended certificate lifespan.
     */
    @SerializedName("recommended_certificate_lifespan")
    private long recommendedCertificateLifespan;

    /**
     * The RSA key size.
     */
    @SerializedName("rsa_key_size")
    private Long rsaKeySize;

    /**
     * The list of certificate curves.
     */
    @SerializedName("certificate_curves")
    private List<String> certificateCurves;

    /**
     * The size of the DH parameter.
     */
    @SerializedName("dh_param_size")
    private Long dhParamSize;

    /**
     * The list of supported TLS versions.
     */
    @SerializedName("tls_versions")
    private List<String> tlsVersions;

    /**
     * The minimum age for HSTS.
     */
    @SerializedName("hsts_min_age")
    private long hstsMinAge;

    /**
     * The cipher suites configuration.
     */
    @SerializedName("ciphers")
    private Ciphers ciphers;

    /**
     * Indicates if OCSP stapling is enabled.
     */
    @SerializedName("ocsp_staple")
    private boolean ocspStaple;

    /**
     * The list of supported certificate signatures.
     */
    @SerializedName("certificate_signatures")
    private List<String> certificateSignatures;

    /**
     * The list of supported TLS curves.
     */
    @SerializedName("tls_curves")
    private List<String> tlsCurves;

    /**
     * The list of supported certificate types.
     */
    @SerializedName("certificate_types")
    private List<String> certificateTypes;
}
