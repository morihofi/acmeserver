/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.api.abstractclass;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public abstract class AbstractAcmeEndpoint implements Handler {

    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;

    /**
     * Gson for JSON to POJO and POJO to JSON conversion
     */
    private final Gson gson;

    public AbstractAcmeEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
    }

    public Provisioner getProvisioner() {
        return provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        handleRequest(ctx, provisioner, gson, acmeRequestBody);
    }

    public abstract void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception;

    public void performSignatureAndNonceCheck(Context ctx, String accountId, ACMERequestBody acmeRequestBody) {
        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, accountId, gson);
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());
    }

    public void performSignatureAndNonceCheck(Context ctx, ACMEAccount account, ACMERequestBody acmeRequestBody) {
        performSignatureAndNonceCheck(ctx, account.getAccountId(), acmeRequestBody);
    }
}
