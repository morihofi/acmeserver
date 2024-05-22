/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents the authorization response in the context of the {@link AuthzOwnershipEndpoint}. This class encapsulates details of the
 * authorization response, such as its status, expiration time, associated identifier, and a list of challenges for validation.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class AuthzResponse {
    private String status;
    private String expires;
    private Identifier identifier;
    private List<ChallengeResponse> challenges;

    /**
     * Default constructor for AuthzResponse.
     */
    public AuthzResponse() {
    }

    /**
     * Retrieves the status of the authorization. The status indicates the current state of the authorization, such as pending or valid.
     *
     * @return The status of the authorization as a {@code String}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the authorization. This method allows updating the state of the authorization.
     *
     * @param status The new status of the authorization as a {@code String}.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the expiration time of the authorization. This time indicates when the authorization will no longer be valid.
     *
     * @return The expiration time as a {@code String}.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Sets the expiration time of the authorization. This method allows specifying when the authorization should expire.
     *
     * @param expires The new expiration time as a {@code String}.
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Retrieves the identifier associated with the authorization. The identifier typically represents the entity (such as a domain name)
     * being authorized.
     *
     * @return The {@link Identifier} associated with the authorization.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier associated with the authorization. This method allows changing the entity being authorized.
     *
     * @param identifier The {@link Identifier} to associate with the authorization.
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    /**
     * Retrieves the list of challenges for the authorization. These challenges are used to validate control over the identifier.
     *
     * @return A list of {@link ChallengeResponse} objects representing the challenges for authorization.
     */
    public List<ChallengeResponse> getChallenges() {
        return challenges;
    }

    /**
     * Sets the list of challenges for the authorization. This method allows specifying the challenges that need to be completed for the
     * authorization.
     *
     * @param challenges A list of {@link ChallengeResponse} objects to set as the authorization challenges.
     */
    public void setChallenges(List<ChallengeResponse> challenges) {
        this.challenges = challenges;
    }
}
