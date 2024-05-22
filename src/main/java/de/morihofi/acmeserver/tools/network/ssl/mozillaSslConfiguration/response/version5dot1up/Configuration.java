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

package de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version5dot1up;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Configuration {
    @SerializedName("ecdh_param_size")
    private long ecdhParamSize;
    @SerializedName("oldest_clients")
    private List<String> oldestClients;
    @SerializedName("server_preferred_order")
    private boolean serverPreferredOrder;
    @SerializedName("ciphersuites")
    private List<String> ciphersuites;
    @SerializedName("maximum_certificate_lifespan")
    private long maximumCertificateLifespan;
    @SerializedName("recommended_certificate_lifespan")
    private long recommendedCertificateLifespan;
    @SerializedName("rsa_key_size")
    private Long rsaKeySize;
    @SerializedName("certificate_curves")
    private List<String> certificateCurves;
    @SerializedName("dh_param_size")
    private Long dhParamSize;
    @SerializedName("tls_versions")
    private List<String> tlsVersions;
    @SerializedName("hsts_min_age")
    private long hstsMinAge;
    @SerializedName("ciphers")
    private Ciphers ciphers;
    @SerializedName("ocsp_staple")
    private boolean ocspStaple;
    @SerializedName("certificate_signatures")
    private List<String> certificateSignatures;
    @SerializedName("tls_curves")
    private List<String> tlsCurves;
    @SerializedName("certificate_types")
    private List<String> certificateTypes;

    public long getEcdhParamSize() {
        return ecdhParamSize;
    }

    public List<String> getOldestClients() {
        return oldestClients;
    }

    public boolean isServerPreferredOrder() {
        return serverPreferredOrder;
    }

    public List<String> getCiphersuites() {
        return ciphersuites;
    }

    public long getMaximumCertificateLifespan() {
        return maximumCertificateLifespan;
    }

    public long getRecommendedCertificateLifespan() {
        return recommendedCertificateLifespan;
    }

    public Long getRsaKeySize() {
        return rsaKeySize;
    }

    public List<String> getCertificateCurves() {
        return certificateCurves;
    }

    public Long getDhParamSize() {
        return dhParamSize;
    }

    public List<String> getTlsVersions() {
        return tlsVersions;
    }

    public long getHstsMinAge() {
        return hstsMinAge;
    }

    public Ciphers getCiphers() {
        return ciphers;
    }

    public boolean isOcspStaple() {
        return ocspStaple;
    }

    public List<String> getCertificateSignatures() {
        return certificateSignatures;
    }

    public List<String> getTlsCurves() {
        return tlsCurves;
    }

    public List<String> getCertificateTypes() {
        return certificateTypes;
    }
}
