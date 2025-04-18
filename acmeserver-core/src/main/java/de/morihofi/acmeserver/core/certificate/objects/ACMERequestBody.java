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

package de.morihofi.acmeserver.core.certificate.objects;

import com.google.gson.annotations.SerializedName;
import de.morihofi.acmeserver.core.tools.base64.Base64Tools;

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
    @SerializedName("payload")
    private String payload;

    /**
     * The signature of the ACME request, encoded in Base64.
     */
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
     * Retrieves the raw, Base64-encoded payload of the ACME request. The payload contains the actual data of the request.
     *
     * @return The Base64-encoded payload as a {@code String}.
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Retrieves the decoded value of the payload. The payload is Base64-encoded and contains the actual data of the request.
     *
     * @return The decoded payload as a {@code String}.
     */
    public String getDecodedPayload() {
        return Base64Tools.decodeBase64(payload);
    }

    /**
     * Retrieves the raw, Base64-encoded signature of the ACME request. The signature verifies the authenticity and integrity of the
     * protected header and payload.
     *
     * @return The Base64-encoded signature as a {@code String}.
     */
    public String getSignature() {
        return signature;
    }
}
