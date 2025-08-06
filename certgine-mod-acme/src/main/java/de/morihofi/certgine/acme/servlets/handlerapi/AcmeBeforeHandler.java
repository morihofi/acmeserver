/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi;

import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.certgine.types.httpserver.HandlerType;

public class AcmeBeforeHandler implements Handler {

    @Override
    public void handle(HandlerContext context) throws Exception {
        boolean isPostRequest = context.method() == HandlerType.POST;
        boolean isAcmeApiPath = context.path().startsWith("/acme/");
        boolean isInvalidContentType = !"application/jose+json".equals(context.contentType());

        // Only for POST requests to ACME API with incorrect content type
        if (isPostRequest && isAcmeApiPath && isInvalidContentType) {
            throw new ACMEMalformedException(
                    "Invalid Content-Type header on POST. " +
                            "Content-Type must be \"application/jose+json\""
            );
        }
    }
}
