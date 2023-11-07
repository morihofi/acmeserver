package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.tools.Crypto;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class OrderCertEndpoint implements Handler {

    private Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public OrderCertEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }


    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/pem-certificate-chain");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");

        ACMEIdentifier identifier = Database.getACMEIdentifierByOrderId(orderId);

        String responseCertificateChain = Database.getCertificateChainPEMofACMEbyAuthorizationId(identifier.getAuthorizationId());

        ctx.result(responseCertificateChain);
    }
}
