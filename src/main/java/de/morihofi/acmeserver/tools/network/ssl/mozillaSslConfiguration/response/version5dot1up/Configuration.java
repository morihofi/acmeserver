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

