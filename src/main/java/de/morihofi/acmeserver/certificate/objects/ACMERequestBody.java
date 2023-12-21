package de.morihofi.acmeserver.certificate.objects;

import com.google.gson.annotations.SerializedName;
import de.morihofi.acmeserver.tools.base64.Base64Tools;

public class ACMERequestBody {
    @SerializedName("protected")
    private String protectedHeader;
    @SerializedName("payload")
    private String payload;
    @SerializedName("signature")
    private String signature;

    public String getDecodedProtected() {
        return Base64Tools.decodeBase64(protectedHeader);
    }

    public String getProtected() {
        return protectedHeader;
    }

    public String getPayload() {
        return payload;
    }

    public String getDecodedPayload() {
        return Base64Tools.decodeBase64(payload);
    }

    public String getSignature() {
        return signature;
    }
}
