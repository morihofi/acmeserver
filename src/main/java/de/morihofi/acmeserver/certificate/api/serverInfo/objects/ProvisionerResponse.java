package de.morihofi.acmeserver.certificate.api.serverInfo.objects;

/**
 * Represents the response structure for a provisioner.
 * This class encapsulates details about a provisioner, primarily its name.
 */
public class ProvisionerResponse {

    /**
     * Provisioner name
     */
    private String name;

    /**
     * Retrieves the name of the provisioner.
     * This name identifies the provisioner within the system.
     *
     * @return The name of the provisioner as a {@code String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the provisioner.
     * This method allows assigning or changing the name of the provisioner.
     *
     * @param name The new name for the provisioner as a {@code String}.
     */
    public void setName(String name) {
        this.name = name;
    }
}
