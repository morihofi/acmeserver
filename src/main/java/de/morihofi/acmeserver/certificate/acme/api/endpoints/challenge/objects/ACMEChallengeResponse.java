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

package de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.objects;

/**
 * Represents the response for an ACME challenge. This class encapsulates the details required
 * for the ACME challenge operations such as status, verification date, challenge URL, token, and type.
 */
public class ACMEChallengeResponse {

    /**
     * Status of the ACME challenge.
     */
    private String status;

    /**
     * Verified date, formatted as a string.
     */
    private String verified;

    /**
     * URL for challenge approval.
     */
    private String url;

    /**
     * Token to place.
     */
    private String token;

    /**
     * Type of the challenge.
     */
    private String type;

    /**
     * Retrieves the status of the ACME challenge.
     *
     * @return The status of the challenge.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the ACME challenge.
     *
     * @param status The status to set.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the verified date of the ACME challenge.
     *
     * @return The verified date as a string.
     */
    public String getVerified() {
        return verified;
    }

    /**
     * Sets the verified date of the ACME challenge.
     *
     * @param verified The verified date to set.
     */
    public void setVerified(String verified) {
        this.verified = verified;
    }

    /**
     * Retrieves the URL for challenge approval.
     *
     * @return The URL for challenge approval.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL for challenge approval.
     *
     * @param url The URL to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Retrieves the token to place for the ACME challenge.
     *
     * @return The token for the challenge.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token to place for the ACME challenge.
     *
     * @param token The token to set.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Retrieves the type of the ACME challenge.
     *
     * @return The type of the challenge.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the ACME challenge.
     *
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }
}
