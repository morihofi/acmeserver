package de.morihofi.acmeserver.database.objects;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Represents an ACME order entity used for managing certificate orders.
 */
@Entity
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrder implements Serializable {

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());


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

    @Column(name = "notBefore")
    private Timestamp notBefore;

    @Column(name = "notAfter")
    private Timestamp notAfter;

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

    public Timestamp getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Timestamp notBefore) {
        this.notBefore = notBefore;
    }

    public Timestamp getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Timestamp notAfter) {
        this.notAfter = notAfter;
    }

    public void setOrderIdentifiers(List<ACMEOrderIdentifier> orderIdentifiers) {
        this.orderIdentifiers = orderIdentifiers;
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




    /**
     * Retrieves an ACME (Automated Certificate Management Environment) identifier by its associated certificate serial number.
     *
     * @param serialNumber The serial number of the certificate associated with the ACME identifier.
     * @return The ACME identifier matching the provided certificate serial number, or null if not found.
     */
    public static ACMEOrder getACMEOrderCertificateSerialNumber(BigInteger serialNumber) {
        ACMEOrder order = null;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            order = session.createQuery("FROM ACMEOrder WHERE certificateSerialNumber = :certificateSerialNumber", ACMEOrder.class)
                    .setParameter("certificateSerialNumber", serialNumber)
                    .setMaxResults(1)
                    .getSingleResult();

            if (order != null) {
                log.info("Got ACME certificate with serial number {} in database", serialNumber);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Unable get ACME order for certificate serial number id {}", serialNumber, e);
        }
        return order;
    }

    /**
     * Retrieves a ACME Order (Automated Certificate Management Environment) with a specific order ID.
     *
     * @param orderId The unique identifier of the ACME order for which identifiers are to be retrieved.
     * @return A list of ACME identifiers associated with the provided order ID.
     */
    public static ACMEOrder getACMEOrder(String orderId) {
        ACMEOrder order;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            order = session.createQuery("FROM ACMEOrder a WHERE a.orderId = :orderId", ACMEOrder.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();

        }
        return order;
    }

    public static List<ACMEOrder> getAllACMEOrdersWithState(AcmeOrderState orderState) {
        List<ACMEOrder> orders;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            orders = session.createQuery("FROM ACMEOrder a WHERE a.orderState = :orderState", ACMEOrder.class)
                    .setParameter("orderState", orderState)
                    .getResultList();

        }
        return orders;
    }

    /**
     * Retrieves the PEM-encoded certificate chain of an ACME entity by its certificate ID.
     * This method fetches the issued certificate from a database using Hibernate, appends the intermediate certificate,
     * and then appends each certificate in the CA certificate chain. If the issued certificate is not found,
     * it throws an IllegalArgumentException.
     *
     * @param certificateId The authorization ID associated with the ACME entity.
     * @param provisioner   The provisioner instance used for cryptographic operations.
     * @return A string representation of the certificate chain in PEM format.
     * @throws CertificateEncodingException if an error occurs during the encoding of certificates.
     * @throws IOException                  if an I/O error occurs during certificate processing.
     * @throws KeyStoreException            if an error occurs while accessing the keystore.
     */
    public static String getCertificateChainPEMofACMEbyCertificateId(String certificateId, Provisioner provisioner) throws CertificateEncodingException, IOException, KeyStoreException {
        StringBuilder pemBuilder = new StringBuilder();

        // Get Issued certificate
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Query query = session.createQuery("SELECT a FROM ACMEOrder a WHERE a.certificateId = :certificateId", ACMEOrder.class);
            query.setParameter("certificateId", certificateId);
            Object result = query.getSingleResult();

            if (result instanceof ACMEOrder acmeOrder) {


                String certificatePEM = acmeOrder.getCertificatePem();
                Date certificateExpires = acmeOrder.getCertificateExpires();

                if (certificatePEM == null && acmeOrder.getCertificateCSR() == null) {
                    throw new ACMEServerInternalException("No CSR was found in database. Have you already submitted a CSR? You cannot get a certificate without submitting a CSR.");

                }else if(certificatePEM == null){
                    return null; //Returning null if it looks like that the server is generating in background
                }

                log.info("Getting Certificate for authorization Id {} -> Expires at {}", certificateId, certificateExpires);

                pemBuilder.append(certificatePEM);
            }

            transaction.commit();
        }


        //Certificate chain
        log.info("Adding Intermediate and CA certificate");
        pemBuilder.append("\n");

        List<X509Certificate> certificateChain = TypeSafetyHelper.safeCastToClassOfType(
                Arrays.stream(
                        provisioner.getCryptoStoreManager().getKeyStore().getCertificateChain(
                                CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(
                                        provisioner.getProvisionerName()
                                )
                        )
                ).toList(),
                X509Certificate.class);

        for (X509Certificate certificate : certificateChain) {
            pemBuilder.append(PemUtil.certificateToPEM(certificate.getEncoded()));
            pemBuilder.append("\n");
        }


        log.info("Returning certificate chain of intermediate and root-ca {}", certificateChain);

        return pemBuilder.toString();
    }


    /**
     * Retrieves a list of revoked certificates from the database. Revoked certificates are identified by having
     * both a revoke status code and a revoke timestamp in their associated ACME identifiers.
     *
     * @param provisionerName Provisioner to get revoked certificates for
     * @return A list of {@link RevokedCertificate} objects representing the revoked certificates.
     */
    public static List<RevokedCertificate> getRevokedCertificates(String provisionerName) {
        List<RevokedCertificate> certificates = new ArrayList<>();

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            //Certificates are revoked when they have a statusCode and a timestamp
            Query query = session.createQuery("FROM ACMEOrder a WHERE revokeStatusCode IS NOT NULL AND revokeTimestamp IS NOT NULL AND a.account.provisioner = :provisionerName", ACMEOrder.class);
            query.setParameter("provisionerName", provisionerName);
            List<ACMEOrder> result = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), ACMEOrder.class);

            if (!result.isEmpty()) {
                for (ACMEOrder revokedIdentifier : result) {
                    certificates.add(new RevokedCertificate(
                            revokedIdentifier.getCertificateSerialNumber(),
                            revokedIdentifier.getRevokeTimestamp(),
                            revokedIdentifier.getRevokeStatusCode()
                    ));
                }
            }

            transaction.commit();
        } catch (Exception e) {
            log.error("Unable to get revoked certificates", e);
        }

        return certificates;
    }

    /**
     * Revokes an ACME (Automated Certificate Management Environment) certificate associated with an ACME identifier.
     *
     * @param order  The ACME order for which the certificate is to be revoked.
     * @param reason The reason code for revoking the certificate.
     * @throws ACMEServerInternalException If an error occurs while revoking the certificate.
     */
    public static void revokeCertificate(ACMEOrder order, int reason) {
        order.setRevokeTimestamp(Timestamp.from(Instant.now()));
        order.setRevokeStatusCode(reason);

        Transaction transaction;
        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            transaction = session.beginTransaction();

            session.merge(order);

            transaction.commit();
            log.info("Revoked certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(), order.getAccount().getProvisioner());
        } catch (Exception e) {
            log.error("Unable to revoke certificate with serial number {} (Provisioner {})", order.getCertificateSerialNumber(), order.getAccount().getProvisioner(), e);
            throw new ACMEServerInternalException("Unable to revoke certificate");
        }
    }


}
