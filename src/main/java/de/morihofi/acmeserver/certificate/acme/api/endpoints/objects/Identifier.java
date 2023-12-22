package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;

/**
 * ACME Identifier used in Requests from ACME Clients
 */
public class Identifier {

    /**
     * Type of the DNS identifier, mostly <code>dns</code>
     */
    private String type;

    /**
     * Value of the identifier, so it is the DNS Name
     */
    private String value;

    /**
     * Constructs a new Identifier with specified type and value.
     * This constructor initializes the Identifier with a specific type (e.g., 'dns') and its corresponding value.
     *
     * @param type The type of the identifier (e.g., 'dns').
     * @param value The value of the identifier (e.g., a DNS name).
     */
    public Identifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Default constructor for Identifier.
     * Constructs an Identifier with default values for type and value.
     */
    public Identifier() {
    }

    /**
     * Retrieves the type of this identifier.
     * The type usually indicates the category of the identifier (e.g., 'dns' for DNS names).
     *
     * @return The type of the identifier as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this identifier.
     * This method allows changing the type of the identifier.
     *
     * @param type The new type of the identifier as a {@code String}.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the value of this identifier.
     * The value is specific to the type of the identifier (e.g., a DNS name for type 'dns').
     *
     * @return The value of the identifier as a {@code String}.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this identifier.
     * This method allows changing the value of the identifier.
     *
     * @param value The new value of the identifier as a {@code String}.
     */
    public void setValue(String value) {
        this.value = value;
    }
}
