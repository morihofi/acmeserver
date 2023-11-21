package de.morihofi.acmeserver.certificate.acme.api.endpoints.download;

import de.morihofi.acmeserver.Main;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DownloadCaEndpoint implements Handler {

    public final Logger log = LogManager.getLogger(getClass());

    public DownloadCaEndpoint() {
    }
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/x-x509-ca-cert");

        ctx.result(new String(Files.readAllBytes(Main.caCertificatePath), StandardCharsets.UTF_8));
    }
}
