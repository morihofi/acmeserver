package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public class OrderCertEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    public OrderCertEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/pem-certificate-chain");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");

        ACMEOrder order = ACMEOrder.getACMEOrder(orderId);
        StringBuilder responseCertificateChainBuilder = new StringBuilder();

        String individualCertificateChain = ACMEOrder.getCertificateChainPEMofACMEbyCertificateId(
                order.getCertificateId(),
                provisioner
        );
        if(individualCertificateChain != null){
            responseCertificateChainBuilder.append(individualCertificateChain);
            responseCertificateChainBuilder.append("\n"); // Separator between certificates

            String responseCertificateChain = responseCertificateChainBuilder.toString();
            ctx.result(responseCertificateChain);
        }else{
            ctx.status(404); //No certificate yet
            ctx.result("Certificate is being issued, please try in a few moments again");
        }



    }


}
