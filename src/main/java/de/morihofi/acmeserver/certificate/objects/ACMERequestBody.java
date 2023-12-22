package de.morihofi.acmeserver.certificate.objects;

import com.google.gson.annotations.SerializedName;
import de.morihofi.acmeserver.tools.base64.Base64Tools;

/**
 * Represents the body of an ACME (Automated Certificate Management Environment) request.
 * This class encapsulates the components of an ACME request, including the protected header, payload, and signature.
 * Each component is stored in Base64-encoded format.
 */
public class ACMERequestBody {
    @SerializedName("protected")
    private String protectedHeader;
    @SerializedName("payload")
    private String payload;
    @SerializedName("signature")
    private String signature;

    /**
     * Retrieves the decoded value of the protected header.
     * The protected header is Base64-encoded and contains information about the request and the signature algorithm.
     *
     * @return The decoded protected header as a {@code String}.
     */
    public String getDecodedProtected() {
        return Base64Tools.decodeBase64(protectedHeader);
    }

    /**
     * Retrieves the raw, Base64-encoded value of the protected header.
     *
     * @return The Base64-encoded protected header as a {@code String}.
     */
    public String getProtected() {
        return protectedHeader;
    }

    /**
     * Retrieves the raw, Base64-encoded payload of the ACME request.
     * The payload contains the actual data of the request.
     *
     * @return The Base64-encoded payload as a {@code String}.
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Retrieves the decoded value of the payload.
     * The payload is Base64-encoded and contains the actual data of the request.
     *
     * @return The decoded payload as a {@code String}.
     */
    public String getDecodedPayload() {
        return Base64Tools.decodeBase64(payload);
    }

    /**
     * Retrieves the raw, Base64-encoded signature of the ACME request.
     * The signature verifies the authenticity and integrity of the protected header and payload.
     *
     * @return The Base64-encoded signature as a {@code String}.
     */
    public String getSignature() {
        return signature;
    }
}
