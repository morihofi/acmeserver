package de.morihofi.acmeserver.certificate.acme.api;

import de.morihofi.acmeserver.Main;

public class Provisioner {

    /**
     * Get the ACME Server URL, reachable from other Hosts
     *
     * @return Full url (including HTTPS prefix) and port to this server
     */
    public String getApiURL() {
        return "https://" + Main.acmeThisServerDNSName + ":" + Main.acmeThisServerAPIPort + "/" + provisionerName;
    }

    private String provisionerName;

    public Provisioner(String provisionerName) {
        this.provisionerName = provisionerName;
    }

    public String getProvisionerName() {
        return provisionerName;
    }
}
