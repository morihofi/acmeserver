/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities.authority;

import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class RootCa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private CertificateConfig certificateConfig;


    private String internalUuid;

    public static RootCa getForId(@NonNull IServerInstance si, long id){
        RootCa provisioner;
        try (Session s = si.getDatabaseSession()) {
            provisioner = s.createQuery("FROM RootCa r WHERE r.id = :id", RootCa.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
        return provisioner;
    }

    public static RootCa[] getAllRoots(@NonNull IServerInstance si) {
        return getAllRoots(si.getDatabaseSession());
    }

    public static RootCa[] getAllRoots(@NonNull Session s) {
        List<RootCa> provisioners;
        provisioners = s.createQuery("FROM RootCa", RootCa.class).list();
        return provisioners.toArray(new RootCa[0]);
    }

    public static RootCa getForUuid(@NonNull IServerInstance si, @NonNull String uuid) {
        RootCa ca;
        try (Session s = si.getDatabaseSession()) {
            ca = s.createQuery("FROM RootCa r WHERE r.internalUuid = :uuid", RootCa.class)
                    .setParameter("uuid", uuid)
                    .uniqueResult();
        }
        return ca;
    }
}
