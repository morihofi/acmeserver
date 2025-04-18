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

package de.morihofi.acmeserver.core.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;

public class DirectoryEndpoint implements Handler {
    /**
     * Logger
     */

    private final Provisioner provisioner;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DirectoryEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Method for handling the request
     *
     * @param ctx Javalin Context
     */
    @Override
    public void handle(@NotNull Context ctx) {

        // Response is JSON
        ctx.header("Content-Type", "application/json");

        // Create the Gson instance
        Gson gson = new Gson();

        // Create the meta object
        JsonObject metaObject = new JsonObject();
        {
            String website = "about:blank";
            String tos = "about:blank";
            if(provisioner.getAcmeMetadataConfig().getWebsite() != null){
                website = provisioner.getAcmeMetadataConfig().getWebsite().trim();
            }
            if(provisioner.getAcmeMetadataConfig().getTos() != null){
                tos = provisioner.getAcmeMetadataConfig().getTos().trim();
            }

            metaObject.addProperty("website", website);
            metaObject.addProperty("termsOfService", tos);
        }
        // Create the main JSON object
        JsonObject responseJSON = new JsonObject();
        responseJSON.add("meta", metaObject);
        responseJSON.addProperty("newAccount", provisioner.getAcmeApiURL() + "/acme/new-acct");
        responseJSON.addProperty("newNonce", provisioner.getAcmeApiURL() + "/acme/new-nonce");
        responseJSON.addProperty("newOrder", provisioner.getAcmeApiURL() + "/acme/new-order");
        responseJSON.addProperty("revokeCert", provisioner.getAcmeApiURL() + "/acme/revoke-cert");
        responseJSON.addProperty("keyChange", provisioner.getAcmeApiURL() + "/acme/key-change");

        // Convert the JsonObject to a String
        String jsonResponse = gson.toJson(responseJSON);

        ctx.result(jsonResponse);
    }
}
