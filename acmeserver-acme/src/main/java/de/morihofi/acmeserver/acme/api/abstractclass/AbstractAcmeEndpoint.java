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

package de.morihofi.acmeserver.acme.api.abstractclass;

import com.google.gson.Gson;
import de.morihofi.acmeserver.acme.security.SignatureCheck;
import de.morihofi.acmeserver.acme.api.objects.ACMERequestBody;

import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;


/**
 * Abstract class representing an ACME endpoint handler.
 * This class provides common functionality for handling ACME requests,
 * including JSON serialization/deserialization, and signature and nonce checks.
 */
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public abstract class AbstractAcmeEndpoint implements Handler {


    /**
     * Gson instance for JSON to POJO and POJO to JSON conversion.
     */
    private final Gson gson = new Gson();

    private final IServerInstance serverInstance;

    /**
     * Constructs an AbstractAcmeEndpoint with the given provisioner and server instance.
     *
     * @param serverInstance The server instance.
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public AbstractAcmeEndpoint(@NonNull IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    public static AcmeProvisioner getProvisionerFromJavalin(IServerInstance serverInstance, @NonNull HandlerContext ctx) {
        String pName = ctx.pathParam("provisioner");
        AcmeProvisioner provisioner = AcmeProvisioner.getForName(serverInstance, pName);
        if(provisioner == null){
            throw new IllegalArgumentException("Specified Provisioner " + pName + " does not exist");
        }

        return provisioner;
    }

    /**
     * Gets the provisioner instance.
     *
     * @return The provisioner instance.
     */
    public AcmeProvisioner getProvisioner(@NonNull HandlerContext context) {
        return getProvisionerFromJavalin(serverInstance, context);
    }

    /**
     * Handles an incoming HTTP request.
     * This method parses the request body into an ACMERequestBody object and
     * delegates the actual handling to the handleRequest method.
     *
     * @param ctx The context of the HTTP request.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handle(@NonNull HandlerContext ctx) throws Exception {
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);
        handleRequest(ctx, getProvisioner(ctx), gson, acmeRequestBody);
    }

    /**
     * Abstract method to handle the specific ACME request.
     * Subclasses should implement this method to provide custom request handling logic.
     *
     * @param ctx             The context of the HTTP request.
     * @param provisioner     The provisioner instance.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The parsed ACME request body.
     * @throws Exception If an error occurs while handling the request.
     */
    public abstract void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception;

    /**
     * Performs signature and nonce checks for the request.
     * This method verifies the request signature and checks the nonce for replay attacks.
     *
     * @param ctx             The context of the HTTP request.
     * @param accountId       The ID of the ACME account.
     * @param acmeRequestBody The parsed ACME request body.
     */
    public void performSignatureAndNonceCheck(@NonNull HandlerContext ctx, @NonNull String accountId, @NonNull ACMERequestBody acmeRequestBody) {
        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, accountId, gson, getServerInstance());
        serverInstance.getNonceManager().checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());
    }

    /**
     * Performs signature and nonce checks for the request using an AcmeAccount object.
     * This method verifies the request signature and checks the nonce for replay attacks.
     *
     * @param ctx             The context of the HTTP request.
     * @param account         The ACME account object.
     * @param acmeRequestBody The parsed ACME request body.
     */
    public void performSignatureAndNonceCheck(@NonNull HandlerContext ctx, @NonNull AcmeAccount account, @NonNull ACMERequestBody acmeRequestBody) {
        performSignatureAndNonceCheck(ctx, account.getAccountId(), acmeRequestBody);
    }
}
