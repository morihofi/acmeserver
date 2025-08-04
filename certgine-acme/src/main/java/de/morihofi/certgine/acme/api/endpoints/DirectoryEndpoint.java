/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.json.GsonFactory;

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
        Gson gson = GsonFactory.createGson();

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
