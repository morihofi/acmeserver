package de.morihofi.certgine.types.database.entities.acme;

import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.util.List;

/**
 * Represents an External Account Binding key for ACME.
 */
@Entity
@Data
@NoArgsConstructor
@Slf4j
public class AcmeExternalAccountBinding implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String keyId;

    @Column(nullable = false)
    private String hmacKey;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provisioner_id", nullable = false)
    private AcmeProvisioner provisioner;

    @OneToMany(mappedBy = "externalAccountBinding")
    private List<AcmeAccount> accounts;

    /**
     * Returns the External Account Binding for the given key id.
     *
     * @param serverInstance Server instance
     * @param kid            Key id
     * @return Binding or null if not found
     */
    public static AcmeExternalAccountBinding getForKid(@NonNull IServerInstance serverInstance,
                                                       @NonNull String kid) {
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            AcmeExternalAccountBinding binding = session
                    .createQuery("FROM AcmeExternalAccountBinding b WHERE b.keyId = :kid",
                            AcmeExternalAccountBinding.class)
                    .setParameter("kid", kid)
                    .setMaxResults(1)
                    .uniqueResult();
            transaction.commit();
            return binding;
        } catch (Exception e) {
            log.error("Unable to get external account binding {}", kid, e);
            return null;
        }
    }
}

