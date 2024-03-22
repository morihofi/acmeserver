package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class NotImplementedEndpoint implements Handler {
    @Override
    public void handle(@NotNull Context ctx) {
        throw new ACMEServerInternalException("Account key rollover is currently not supported.");
    }
}
