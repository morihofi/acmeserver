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

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.intf.IServerInstance;

import lombok.NonNull;

public class DirectoryEndpoint implements Handler {

    private final IServerInstance serverInstance;

    public DirectoryEndpoint(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Method for handling the request
     *
     * @param ctx Javalin Context
     */
    @Override
    public void handle(@NonNull HandlerContext ctx) {
        AcmeProvisioner provisioner = AbstractAcmeEndpoint.getProvisionerFromJavalin(serverInstance, ctx);

        // Response is JSON
        ctx.header("Content-Type", "application/json");

        // Create the Gson instance
        Gson gson = new Gson();

        // Create the meta object
        JsonObject metaObject = new JsonObject();
        {
            String website = "about:blank";
            String tos = "about:blank";
            if(provisioner.getMeta().getWebsite() != null && !provisioner.getMeta().getWebsite().isEmpty()){
                website = provisioner.getMeta().getWebsite().trim();
            }
            if(provisioner.getMeta().getTos() != null && !provisioner.getMeta().getTos().isEmpty()){
                tos = provisioner.getMeta().getTos().trim();
            }

            metaObject.addProperty("website", website);
            metaObject.addProperty("termsOfService", tos);
        }
        // Create the main JSON object
        JsonObject responseJSON = new JsonObject();
        responseJSON.add("meta", metaObject);
        responseJSON.addProperty("newAccount", provisioner.getAcmeApiURL(serverInstance) + "/acme/new-acct");
        responseJSON.addProperty("newNonce", provisioner.getAcmeApiURL(serverInstance) + "/acme/new-nonce");
        responseJSON.addProperty("newOrder", provisioner.getAcmeApiURL(serverInstance) + "/acme/new-order");
        responseJSON.addProperty("revokeCert", provisioner.getAcmeApiURL(serverInstance) + "/acme/revoke-cert");
        responseJSON.addProperty("keyChange", provisioner.getAcmeApiURL(serverInstance) + "/acme/key-change");

        // Convert the JsonObject to a String
        String jsonResponse = gson.toJson(responseJSON);

        ctx.result(jsonResponse);
    }
}
