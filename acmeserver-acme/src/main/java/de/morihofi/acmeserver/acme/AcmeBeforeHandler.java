package de.morihofi.acmeserver.acme;

import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.types.httpserver.HandlerType;

public class AcmeBeforeHandler implements Handler {
    @Override
    public void handle(HandlerContext context) throws Exception {
        // Check for correct content type, except for ACME directory
        if (
                context.method() == HandlerType.POST && // Only for POST Requests
                        (context.path().startsWith("/acme/") && !"application/jose+json".equals(context.contentType())) // True when ACME API
        ){
            throw new ACMEMalformedException("Invalid Content-Type header on POST. Content-Type must be \"application/jose+json\"");
        }

    }
}
