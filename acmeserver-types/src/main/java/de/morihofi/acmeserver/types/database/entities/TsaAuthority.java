package de.morihofi.acmeserver.types.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;

/**
 * Entity representing a Time Stamp Authority certificate configuration.
 */
@Entity
@Data
@NoArgsConstructor
public class TsaAuthority implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CertificateConfig certificateConfig;

    private String internalUuid;

    /**
     * Retrieve TSA by database id using a server instance.
     */
    public static TsaAuthority getForId(@NonNull IServerInstance si, long id) {
        try (Session s = si.getDatabaseSession()) {
            return getForId(s, id);
        }
    }

    /**
     * Retrieve TSA by database id using an open session.
     */
    public static TsaAuthority getForId(@NonNull Session s, long id) {
        return s.createQuery("FROM TsaAuthority t WHERE t.id = :id", TsaAuthority.class)
                .setParameter("id", id)
                .uniqueResult();
    }

    /**
     * Retrieve TSA by internal UUID using a server instance.
     */
    public static TsaAuthority getForUuid(@NonNull IServerInstance si, @NonNull String uuid) {
        try (Session s = si.getDatabaseSession()) {
            return getForUuid(s, uuid);
        }
    }

    /**
     * Retrieve TSA by internal UUID using an open session.
     */
    public static TsaAuthority getForUuid(@NonNull Session s, @NonNull String uuid) {
        return s.createQuery("FROM TsaAuthority t WHERE t.internalUuid = :uuid", TsaAuthority.class)
                .setParameter("uuid", uuid)
                .uniqueResult();
    }

    /**
     * Retrieve all stored TSA records using a server instance.
     */
    public static TsaAuthority[] getAll(@NonNull IServerInstance si) {
        try (Session s = si.getDatabaseSession()) {
            return getAll(s);
        }
    }

    /**
     * Retrieve all stored TSA records using an open session.
     */
    public static TsaAuthority[] getAll(@NonNull Session s) {
        List<TsaAuthority> tsas = s.createQuery("FROM TsaAuthority", TsaAuthority.class).list();
        return tsas.toArray(new TsaAuthority[0]);
    }
}
