package de.morihofi.acmeserver.certificate.objects;

import com.google.gson.annotations.SerializedName;

public class ACMERequestBody {
    @SerializedName("protected")
    private String protectedHeader;
    @SerializedName("payload")
    private String payload;
    @SerializedName("signature")
    private String signature;

    public String getProtected() {
        return protectedHeader;
    }

    public String getPayload() {
        return payload;
    }

    public String getSignature() {
        return signature;
    }
}
