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

package de.morihofi.acmeserver.database.objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

@Entity
@Table(name = "httpnonces")
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class HttpNonces {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "timestamp", nullable = true) // Column name "timestamp" instead of redeemTimestamp to keep compatibility
    private LocalDateTime redeemTimestamp;

    @Column(name = "generated", nullable = true)
    private LocalDateTime generationTimestamp;

    /**
     * Constructs a new nonce with the given nonce string and timestamp.
     *
     * @param nonce     The nonce string.
     */
    public HttpNonces(String nonce) {
        this.nonce = nonce;
        this.redeemTimestamp = null;
        this.generationTimestamp = LocalDateTime.now();
    }

    public HttpNonces() {
    }

    /**
     * Get the nonce string.
     *
     * @return The nonce string.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Set the nonce string.
     *
     * @param nonce The nonce string to set.
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Get the timestamp when the nonce was redeemed.
     *
     * @return The timestamp.
     */
    public LocalDateTime getRedeemTimestamp() {
        return redeemTimestamp;
    }

    /**
     * Set the timestamp when the nonce was redeemed.
     *
     * @param redeemTimestamp The timestamp to set.
     */
    public void setRedeemTimestamp(LocalDateTime redeemTimestamp) {
        this.redeemTimestamp = redeemTimestamp;
    }

    public LocalDateTime getGenerationTimestamp() {
        return generationTimestamp;
    }

    public void setGenerationTimestamp(LocalDateTime generationTimestamp) {
        this.generationTimestamp = generationTimestamp;
    }
}
