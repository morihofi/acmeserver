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
