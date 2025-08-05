/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account.objects;

import com.google.gson.annotations.SerializedName;
import de.morihofi.certgine.utils.base64.Base64Tools;
import lombok.Getter;

/**
 * Represents the externalAccountBinding object used in newAccount requests.
 */
@Getter
public class ExternalAccountBinding {
    @SerializedName("protected")
    private String protectedHeader;
    private String payload;
    private String signature;

    public String getDecodedProtected() {
        return Base64Tools.decodeBase64(protectedHeader);
    }

    public String getDecodedPayload() {
        return Base64Tools.decodeBase64(payload);
    }
}
