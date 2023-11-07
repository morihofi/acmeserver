package de.morihofi.acmeserver.certificate.revoke;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class CRLEndpoint implements Handler {

    private Provisioner provisioner;
    private CRL crl;

    public CRLEndpoint(Provisioner provisioner, CRL crl) {
        this.provisioner = provisioner;
        this.crl = crl;
    }


    @Override
    public void handle(Context ctx) throws Exception {
        ctx.status(200);
        ByteBuffer buffer = ByteBuffer.wrap(crl.getCurrentCrlBytes());

        ctx.header("Content-Type", "application/pkix-crl");
        ctx.header("Content-Length", String.valueOf(buffer.capacity()));

        try (OutputStream out = ctx.res().getOutputStream()) {
            out.write(buffer.array());
        }

    }
}
