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

package de.morihofi.acmeserver.certificate.acme.challenges;

/**
 * This enum holds ACME Challenge Types as specified in RFC 8555
 */
public enum AcmeChallengeType {
    /**
     * HTTP-01 Challenge
     */
    HTTP_01("http-01"),
    /**
     * DNS-01 Challenge
     */
    DNS_01("dns-01"),
    /**
     * TLS-ALPN-01 Challenge (unsupported, maybe implemented in future)
     */
    TLS_ALPN_01("tls-alpn-01");

    /**
     * ACME RFC 8555 friendly name of the challenge
     */
    private final String name;

    /**
     * Constructor for a Challenge Type
     * @param name RFC friendly name
     */
    AcmeChallengeType(String name) {
        this.name = name;
    }

    /**
     * Gets the ACME RFC friendly name of the challenge
     * @return RFC friendly name
     */
    public String getName() {
        return name;
    }
}
