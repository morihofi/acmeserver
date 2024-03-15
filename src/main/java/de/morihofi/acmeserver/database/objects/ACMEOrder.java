package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.database.AcmeOrderState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;

/**
 * Represents an ACME order entity used for managing certificate orders.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrder implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderId", unique = true)
    private String orderId;

    @ManyToOne
    @JoinColumn(name = "accountId", referencedColumnName = "accountId")
    private ACMEAccount account;

    @Column(name = "created")
    private Timestamp created;

    @Column(name = "expires")
    private Timestamp expires;

    @OneToMany(mappedBy = "order")
    private List<ACMEOrderIdentifier> orderIdentifiers;

    @Column(name = "orderState", nullable = false)
    @Enumerated(EnumType.STRING)
    private AcmeOrderState orderState = AcmeOrderState.IDLE;


    @Column(name = "certificateId", columnDefinition = "TEXT")
    private String certificateId;

    @Column(name = "certificateCSR", columnDefinition = "TEXT")
    private String certificateCSR;

    @Column(name = "certificateIssued")
    private Timestamp certificateIssued;

    @Column(name = "certificateExpires")
    private Timestamp certificateExpires;

    @Column(name = "certificatePem", columnDefinition = "TEXT")
    private String certificatePem;

    @Column(name = "certificateSerialNumber", precision = 50, scale = 0)
    private BigInteger certificateSerialNumber;

    @Column(name = "revokeStatusCode", nullable = true)
    private Integer revokeStatusCode;

    @Column(name = "revokeTimestamp", nullable = true)
    private Timestamp revokeTimestamp;


    /**
     * Get the unique identifier of the ACME order.
     * @return The order ID.
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Set the unique identifier of the ACME order.
     * @param orderId The order ID to set.
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Get the ACME account associated with this order.
     * @return The ACME account.
     */
    public ACMEAccount getAccount() {
        return account;
    }

    /**
     * Set the ACME account associated with this order.
     * @param account The ACME account to set.
     */
    public void setAccount(ACMEAccount account) {
        this.account = account;
    }

    /**
     * Get the timestamp when the ACME order was created.
     * @return The creation timestamp.
     */
    public Timestamp getCreated() {
        return created;
    }

    /**
     * Set the timestamp when the ACME order was created.
     * @param created The creation timestamp to set.
     */
    public void setCreated(Timestamp created) {
        this.created = created;
    }

    /**
     * Get the certificate ID associated with this ACME identifier.
     * @return The certificate ID.
     */
    public String getCertificateId() {
        return certificateId;
    }

    /**
     * Set the certificate ID associated with this ACME identifier.
     * @param certificateId The certificate ID to set.
     */
    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    /**
     * Get the certificate CSR (Certificate Signing Request) associated with this ACME identifier.
     * @return The certificate CSR.
     */
    public String getCertificateCSR() {
        return certificateCSR;
    }

    /**
     * Set the certificate CSR (Certificate Signing Request) associated with this ACME identifier.
     * @param certificateCSR The certificate CSR to set.
     */
    public void setCertificateCSR(String certificateCSR) {
        this.certificateCSR = certificateCSR;
    }

    /**
     * Get the timestamp when the certificate associated with this ACME identifier was issued.
     * @return The timestamp when the certificate was issued.
     */
    public Timestamp getCertificateIssued() {
        return certificateIssued;
    }

    /**
     * Set the timestamp when the certificate associated with this ACME identifier was issued.
     * @param certificateIssued The timestamp when the certificate was issued to set.
     */
    public void setCertificateIssued(Timestamp certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    /**
     * Get the timestamp when the certificate associated with this ACME identifier expires.
     * @return The timestamp when the certificate expires.
     */
    public Timestamp getCertificateExpires() {
        return certificateExpires;
    }

    /**
     * Set the timestamp when the certificate associated with this ACME identifier expires.
     * @param certificateExpires The timestamp when the certificate expires to set.
     */
    public void setCertificateExpires(Timestamp certificateExpires) {
        this.certificateExpires = certificateExpires;
    }

    /**
     * Get the PEM-encoded certificate associated with this ACME identifier.
     * @return The PEM-encoded certificate.
     */
    public String getCertificatePem() {
        return certificatePem;
    }

    /**
     * Set the PEM-encoded certificate associated with this ACME identifier.
     * @param certificatePem The PEM-encoded certificate to set.
     */
    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    /**
     * Get the serial number of the certificate associated with this ACME identifier.
     * @return The certificate's serial number.
     */
    public BigInteger getCertificateSerialNumber() {
        return certificateSerialNumber;
    }

    /**
     * Set the serial number of the certificate associated with this ACME identifier.
     * @param certificateSerialNumber The certificate's serial number to set.
     */
    public void setCertificateSerialNumber(BigInteger certificateSerialNumber) {
        this.certificateSerialNumber = certificateSerialNumber;
    }

    /**
     * Get the revoke status code associated with this ACME identifier.
     * @return The revoke status code.
     */
    public Integer getRevokeStatusCode() {
        return revokeStatusCode;
    }

    /**
     * Set the revoke status code associated with this ACME identifier.
     * @param revokeStatusCode The revoke status code to set.
     */
    public void setRevokeStatusCode(Integer revokeStatusCode) {
        this.revokeStatusCode = revokeStatusCode;
    }

    /**
     * Get the timestamp when this ACME identifier was revoked.
     * @return The timestamp when the identifier was revoked.
     */
    public Timestamp getRevokeTimestamp() {
        return revokeTimestamp;
    }

    /**
     * Set the timestamp when this ACME identifier was revoked.
     * @param revokeTimestamp The timestamp when the identifier was revoked to set.
     */
    public void setRevokeTimestamp(Timestamp revokeTimestamp) {
        this.revokeTimestamp = revokeTimestamp;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getExpires() {
        return expires;
    }

    public void setExpires(Timestamp expires) {
        this.expires = expires;
    }

    public List<ACMEOrderIdentifier> getOrderIdentifiers() {
        return orderIdentifiers;
    }

    public AcmeOrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(AcmeOrderState orderState) {
        this.orderState = orderState;
    }
}
