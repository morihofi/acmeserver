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
package de.morihofi.acmeserver.api.provisioner.statistics.responses;

public class ProvisionerStatisticResponse {
    private String name;
    private long acmeAccounts;
    private long certificatesIssued;
    private long certificatesRevoked;
    private long certificatesIssueWaiting;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAcmeAccounts() {
        return acmeAccounts;
    }

    public void setAcmeAccounts(long acmeAccounts) {
        this.acmeAccounts = acmeAccounts;
    }

    public long getCertificatesIssued() {
        return certificatesIssued;
    }

    public void setCertificatesIssued(long certificatesIssued) {
        this.certificatesIssued = certificatesIssued;
    }

    public long getCertificatesRevoked() {
        return certificatesRevoked;
    }

    public void setCertificatesRevoked(long certificatesRevoked) {
        this.certificatesRevoked = certificatesRevoked;
    }

    public long getCertificatesIssueWaiting() {
        return certificatesIssueWaiting;
    }

    public void setCertificatesIssueWaiting(long certificatesIssueWaiting) {
        this.certificatesIssueWaiting = certificatesIssueWaiting;
    }
}
