package de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects;

import java.util.List;

public class AccountResponse {
    private String status;
    private List<String> contact;
    private String orders;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getContact() {
        return contact;
    }

    public void setContact(List<String> contact) {
        this.contact = contact;
    }

    public String getOrders() {
        return orders;
    }

    public void setOrders(String orders) {
        this.orders = orders;
    }
}
