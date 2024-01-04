package de.morihofi.acmeserver.database.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "httpNonces")
public class HttpNonces {

    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * Default constructor required for Hibernate.
     */
    public HttpNonces() {
    }

    /**
     * Constructs a new nonce with the given nonce string and timestamp.
     * @param nonce The nonce string.
     * @param timestamp The timestamp when the nonce was generated.
     */
    public HttpNonces(String nonce, LocalDateTime timestamp) {
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    /**
     * Get the nonce string.
     * @return The nonce string.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Set the nonce string.
     * @param nonce The nonce string to set.
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Get the timestamp when the nonce was generated.
     * @return The timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp when the nonce was generated.
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
