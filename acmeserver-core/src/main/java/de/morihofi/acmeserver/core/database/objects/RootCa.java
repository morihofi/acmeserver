package de.morihofi.acmeserver.core.database.objects;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.Session;

import java.util.List;
import java.util.Objects;

@Entity
@Data
public class RootCa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CertificateConfig certificateConfig;

    @OneToMany(mappedBy = "rootCa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcmeProvisioner> provisioners;


    private String internalUuid;

    public static RootCa getForId(ServerInstance si, long id){
        RootCa provisioner;
        try (Session s = Objects.requireNonNull(si.getHibernateUtil().getSessionFactory()).openSession()) {
            provisioner = s.createQuery("FROM RootCa r WHERE r.id = :id", RootCa.class)
                    .setParameter("id", id)
                    .getSingleResult();
        }
        return provisioner;
    }

    public static RootCa[] getAllRoots(ServerInstance si) {
        return getAllRoots(si.getHibernateUtil());
    }
    public static RootCa[] getAllRoots(HibernateUtil hu) {
        List<RootCa> provisioners;
        try (Session s = Objects.requireNonNull(hu.getSessionFactory()).openSession()) {
            provisioners = s.createQuery("FROM RootCa", RootCa.class).list();
        }
        return provisioners.toArray(new RootCa[0]);
    }
}
