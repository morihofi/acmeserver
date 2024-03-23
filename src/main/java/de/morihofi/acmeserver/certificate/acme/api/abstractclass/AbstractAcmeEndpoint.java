package de.morihofi.acmeserver.certificate.acme.api.abstractclass;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

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

    public void performSignatureAndNonceCheck(Context ctx, ACMEAccount account, ACMERequestBody acmeRequestBody) throws NoSuchAlgorithmException, SignatureException, IOException, InvalidKeySpecException, InvalidKeyException, NoSuchProviderException {
        performSignatureAndNonceCheck(ctx, account.getAccountId(), acmeRequestBody);
    }
}
