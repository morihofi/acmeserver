package de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects;

public class FinalizeOrderRequestPayload {
    private String csr;

    public String getCsr() {
        return csr;
    }
}
