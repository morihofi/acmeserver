package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects;

/**
 * Represents a challenge in the context of certificate issuance and validation.
 * This class encapsulates details of a challenge, such as its type, URL, token, status, and validation information.
 */
public class ChallengeResponse {
    private String type;
    private String url;
    private String token;
    private String status;
    private String validated;

    /**
     * Retrieves the type of the challenge.
     * The challenge type typically indicates the method of validation, such as DNS or HTTP.
     *
     * @return The type of the challenge as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the challenge.
     * This method allows specifying the method of validation for the challenge.
     *
     * @param type The challenge type as a {@code String}.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the URL associated with the challenge.
     * This URL may be used to perform or verify the challenge.
     *
     * @return The URL of the challenge as a {@code String}.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL associated with the challenge.
     * This method allows specifying the URL for performing or verifying the challenge.
     *
     * @param url The challenge URL as a {@code String}.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Retrieves the token of the challenge.
     * The token is typically a unique identifier or key for the challenge.
     *
     * @return The challenge token as a {@code String}.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token of the challenge.
     * This method allows assigning a unique identifier or key for the challenge.
     *
     * @param token The challenge token as a {@code String}.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Retrieves the current status of the challenge.
     * The status indicates the progress or outcome of the challenge, such as pending or valid.
     *
     * @return The status of the challenge as a {@code String}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the challenge.
     * This method allows updating the progress or outcome status of the challenge.
     *
     * @param status The challenge status as a {@code String}.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the validation timestamp of the challenge.
     * This timestamp indicates when the challenge was successfully validated.
     *
     * @return The validation timestamp of the challenge as a {@code String}.
     */
    public String getValidated() {
        return validated;
    }

    /**
     * Sets the validation timestamp of the challenge.
     * This method allows specifying when the challenge was successfully validated.
     *
     * @param validated The validation timestamp as a {@code String}.
     */
    public void setValidated(String validated) {
        this.validated = validated;
    }
}
