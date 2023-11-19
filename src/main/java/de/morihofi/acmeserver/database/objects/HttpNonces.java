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

    // Konstruktor ohne Argumente ist erforderlich
    public HttpNonces() {
    }

    // Konstruktor mit Argumenten
    public HttpNonces(String nonce, LocalDateTime timestamp) {
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    // Getter und Setter für nonce und timestamp
    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
