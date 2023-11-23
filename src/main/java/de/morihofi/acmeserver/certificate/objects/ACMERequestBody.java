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
        String decoded = Base64Tools.decodeBase64(protectedHeader);
        return decoded;
    }

    public String getProtected() {
        return protectedHeader;
    }

    public String getPayload() {
        return payload;
    }

    public String getDecodedPayload() {
        String decoded = Base64Tools.decodeBase64(payload);
        return decoded;
    }

    public String getSignature() {
        return signature;
    }
}
