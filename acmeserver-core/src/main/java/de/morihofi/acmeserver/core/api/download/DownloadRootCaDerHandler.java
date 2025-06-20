package de.morihofi.acmeserver.core.api.download;

import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.NonNull;

/**
 * Handler returning the root certificate in DER format for a specific CA.
 */
public class DownloadRootCaDerHandler implements Handler {
    private final IServerInstance serverInstance;

    public DownloadRootCaDerHandler(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    @Override
    public void handle(@NonNull HandlerContext ctx) throws Exception {
        String uuid = ctx.pathParam("uuid");
        RootCa ca = RootCa.getForUuid(serverInstance, uuid);
        if (ca == null) {
            ctx.status(404);
            return;
        }
        ctx.header("Content-Type", "application/x-x509-ca-cert");
        byte[] der = serverInstance.getCryptoStoreManager()
                .getCerificateAuthorityX509Certificate(ca)
                .getEncoded();
        ctx.result(der);
    }
}
