/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf.handler.common;

import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.HttpStatusCode;

/**
 * Handler for handling HTTP OPTIONS requests.
 * This handler sets the response status to 204 (No Content) and provides an empty response body.
 */
public class OptionsHandler implements Handler {

    /**
     * Handles the request by setting the response status to 204 (No Content)
     * and providing an empty response body to indicate the request has been processed successfully.
     *
     * @param context The {@code HandlerContext} that provides information about the current request and response.
     */
    @Override
    public void handle(HandlerContext context) {
        if(context.router().isAnyHandlerRegisteredForPath(context.path())) {
            context.status(HttpStatusCode.NO_CONTENT);
        }else{
            context.status(HttpStatusCode.NOT_FOUND);
        }
        context.result();

    }
}