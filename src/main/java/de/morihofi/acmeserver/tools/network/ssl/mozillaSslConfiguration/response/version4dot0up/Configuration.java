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

package de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version4dot0up;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Configuration {
    @SerializedName("ecdh_param_size")
    private long ecdhParamSize;
    @SerializedName("oldest_clients")
    private List<String> oldestClients;
    @SerializedName("tls_versions")
    private List<String> tlsVersions;
    @SerializedName("dh_param_size")
    private Long dhParamSize;
    @SerializedName("openssl_ciphersuites")
    private String opensslCiphersuites;
    @SerializedName("hsts_min_age")
    private long hstsMinAge;
    @SerializedName("certificate_signatures")
    private List<String> certificateSignatures;
    @SerializedName("tls_curves")
    private List<String> tlsCurves;
    @SerializedName("ciphersuites")
    private List<String> ciphersuites;
    @SerializedName("certificate_types")
    private List<String> certificateTypes;
    @SerializedName("rsa_key_size")
    private long rsaKeySize;
    @SerializedName("certificate_curves")
    private List<String> certificateCurves;

    public long getEcdhParamSize() {
        return ecdhParamSize;
    }

    public List<String> getOldestClients() {
        return oldestClients;
    }

    public List<String> getTlsVersions() {
        return tlsVersions;
    }

    public Long getDhParamSize() {
        return dhParamSize;
    }

    public String getOpensslCiphersuites() {
        return opensslCiphersuites;
    }

    public long getHstsMinAge() {
        return hstsMinAge;
    }

    public List<String> getCertificateSignatures() {
        return certificateSignatures;
    }

    public List<String> getTlsCurves() {
        return tlsCurves;
    }

    public List<String> getCiphersuites() {
        return ciphersuites;
    }

    public List<String> getCertificateTypes() {
        return certificateTypes;
    }

    public long getRsaKeySize() {
        return rsaKeySize;
    }

    public List<String> getCertificateCurves() {
        return certificateCurves;
    }
}
