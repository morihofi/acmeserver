/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.objects;

import com.google.gson.annotations.SerializedName;
import de.morihofi.certgine.utils.base64.Base64Tools;
import lombok.Getter;

/**
 * Represents the body of an ACME (Automated Certificate Management Environment) request. This class encapsulates the components of an ACME
 * request, including the protected header, payload, and signature. Each component is stored in Base64-encoded format.
 */
public class ACMERequestBody {
    /**
     * The protected header of the ACME request, encoded in Base64.
     */
    @SerializedName("protected")
    private String protectedHeader;

    /**
     * The payload of the ACME request, encoded in Base64.
     */
    @Getter
    @SerializedName("payload")
    private String payload;

    /**
     * The signature of the ACME request, encoded in Base64.
     */
    @Getter
    @SerializedName("signature")
    private String signature;

    /**
     * Retrieves the decoded value of the protected header. The protected header is Base64-encoded and contains information about the
     * request and the signature algorithm.
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
     * Retrieves the decoded value of the payload. The payload is Base64-encoded and contains the actual data of the request.
     *
     * @return The decoded payload as a {@code String}.
     */
    public String getDecodedPayload() {
        return Base64Tools.decodeBase64(payload);
    }

}
