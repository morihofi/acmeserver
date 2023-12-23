package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OrderCertEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    public final Logger log = LogManager.getLogger(getClass());

    public OrderCertEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/pem-certificate-chain");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");

        List<ACMEIdentifier> identifiers = Database.getACMEIdentifiersByOrderId(orderId);
        StringBuilder responseCertificateChainBuilder = new StringBuilder();

        for (ACMEIdentifier identifier : identifiers) {
            String individualCertificateChain = Database.getCertificateChainPEMofACMEbyAuthorizationId(
                    identifier.getAuthorizationId(),
                    provisioner.getIntermediateCaCertificate().getEncoded(),
                    provisioner
            );

            responseCertificateChainBuilder.append(individualCertificateChain);
            responseCertificateChainBuilder.append("\n"); // Separator zwischen den Zertifikaten
        }

        String responseCertificateChain = responseCertificateChainBuilder.toString();
        ctx.result(responseCertificateChain);
    }


}
