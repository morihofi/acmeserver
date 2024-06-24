/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects;

/**
 * Represents a challenge in the context of certificate issuance and validation. This class encapsulates details of a challenge, such as its
 * type, URL, token, status, and validation information.
 */
public class ChallengeResponse {
    /**
     * The type of the challenge, indicating the method of validation (e.g., DNS or HTTP).
     */
    private String type;

    /**
     * The URL associated with the challenge, used for performing or verifying the challenge.
     */
    private String url;

    /**
     * The token of the challenge, typically a unique identifier or key for the challenge.
     */
    private String token;

    /**
     * The current status of the challenge, indicating progress or outcome (e.g., pending or valid).
     */
    private String status;

    /**
     * The validation timestamp of the challenge, indicating when it was successfully validated.
     */
    private String validated;

    /**
     * Retrieves the type of the challenge. The challenge type typically indicates the method of validation, such as DNS or HTTP.
     *
     * @return The type of the challenge as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the challenge. This method allows specifying the method of validation for the challenge.
     *
     * @param type The challenge type as a {@code String}.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the URL associated with the challenge. This URL may be used to perform or verify the challenge.
     *
     * @return The URL of the challenge as a {@code String}.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL associated with the challenge. This method allows specifying the URL for performing or verifying the challenge.
     *
     * @param url The challenge URL as a {@code String}.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Retrieves the token of the challenge. The token is typically a unique identifier or key for the challenge.
     *
     * @return The challenge token as a {@code String}.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token of the challenge. This method allows assigning a unique identifier or key for the challenge.
     *
     * @param token The challenge token as a {@code String}.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Retrieves the current status of the challenge. The status indicates the progress or outcome of the challenge, such as pending or
     * valid.
     *
     * @return The status of the challenge as a {@code String}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the challenge. This method allows updating the progress or outcome status of the challenge.
     *
     * @param status The challenge status as a {@code String}.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the validation timestamp of the challenge. This timestamp indicates when the challenge was successfully validated.
     *
     * @return The validation timestamp of the challenge as a {@code String}.
     */
    public String getValidated() {
        return validated;
    }

    /**
     * Sets the validation timestamp of the challenge. This method allows specifying when the challenge was successfully validated.
     *
     * @param validated The validation timestamp as a {@code String}.
     */
    public void setValidated(String validated) {
        this.validated = validated;
    }
}
