/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.abstractclass;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.acme.api.objects.ACMERequestBody;

import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.AcmeAccount;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.certgine.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
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
        if (provisioner == null) {
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
        HttpServletRequest req = ctx.request().getHttpServletRequest();

        // We want to make sure, that all the certificate requests are use done using HTTPS
        if(!req.isSecure()){
            throw new ACMEMalformedException("Requests must be sent over HTTPS for the ACME API");
        }

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
