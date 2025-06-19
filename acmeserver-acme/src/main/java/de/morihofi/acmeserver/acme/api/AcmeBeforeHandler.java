package de.morihofi.acmeserver.acme.api;

import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.types.httpserver.HandlerType;

public class AcmeBeforeHandler implements Handler {

    @Override
    public void handle(HandlerContext context) throws Exception {
        boolean isPostRequest = context.method() == HandlerType.POST;
        boolean isAcmeApiPath = context.path().startsWith("/acme/");
        boolean isInvalidContentType = !"application/jose+json".equals(context.contentType());

        // Only for POST requests to ACME API with incorrect content type
        if (isPostRequest && isAcmeApiPath && isInvalidContentType) {
            throw new ACMEMalformedException(
                    "Invalid Content-Type header on POST. " +
                            "Content-Type must be \"application/jose+json\""
            );
        }
    }
}
