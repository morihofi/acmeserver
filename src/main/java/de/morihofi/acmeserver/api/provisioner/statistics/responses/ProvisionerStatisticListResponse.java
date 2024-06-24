/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.api.provisioner.statistics.responses;

/**
 * Represents the statistics for a list of provisioners, including the name of the provisioner, the number of ACME accounts, the number of certificates issued, the number of certificates revoked, and the number of certificates waiting to be issued.
 */
public class ProvisionerStatisticListResponse {
    /**
     * The name of the provisioner.
     */
    private String name;
    /**
     * The number of ACME accounts associated with the provisioner.
     */
    private long acmeAccounts;
    /**
     * The number of certificates issued by the provisioner.
     */
    private long certificatesIssued;
    /**
     * The number of certificates revoked by the provisioner.
     */
    private long certificatesRevoked;
    /**
     * The number of certificates waiting to be issued by the provisioner.
     */
    private long certificatesIssueWaiting;

    /**
     * Gets the name of the provisioner.
     *
     * @return the name of the provisioner.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the provisioner.
     *
     * @param name the name of the provisioner.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the number of ACME accounts associated with the provisioner.
     *
     * @return the number of ACME accounts.
     */
    public long getAcmeAccounts() {
        return acmeAccounts;
    }

    /**
     * Sets the number of ACME accounts associated with the provisioner.
     *
     * @param acmeAccounts the number of ACME accounts.
     */
    public void setAcmeAccounts(long acmeAccounts) {
        this.acmeAccounts = acmeAccounts;
    }

    /**
     * Gets the number of certificates issued by the provisioner.
     *
     * @return the number of certificates issued.
     */
    public long getCertificatesIssued() {
        return certificatesIssued;
    }

    /**
     * Sets the number of certificates issued by the provisioner.
     *
     * @param certificatesIssued the number of certificates issued.
     */
    public void setCertificatesIssued(long certificatesIssued) {
        this.certificatesIssued = certificatesIssued;
    }

    /**
     * Gets the number of certificates revoked by the provisioner.
     *
     * @return the number of certificates revoked.
     */
    public long getCertificatesRevoked() {
        return certificatesRevoked;
    }

    /**
     * Sets the number of certificates revoked by the provisioner.
     *
     * @param certificatesRevoked the number of certificates revoked.
     */
    public void setCertificatesRevoked(long certificatesRevoked) {
        this.certificatesRevoked = certificatesRevoked;
    }

    /**
     * Gets the number of certificates waiting to be issued by the provisioner.
     *
     * @return the number of certificates waiting to be issued.
     */
    public long getCertificatesIssueWaiting() {
        return certificatesIssueWaiting;
    }

    /**
     * Sets the number of certificates waiting to be issued by the provisioner.
     *
     * @param certificatesIssueWaiting the number of certificates waiting to be issued.
     */
    public void setCertificatesIssueWaiting(long certificatesIssueWaiting) {
        this.certificatesIssueWaiting = certificatesIssueWaiting;
    }
}
