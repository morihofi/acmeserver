package de.morihofi.acmeserver.certificate.provisioners;

import de.morihofi.acmeserver.database.HibernateUtil;
import jakarta.persistence.Query;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProvisionerStatistics {
    public static Map<LocalDate, Long> getCertificatesIssuedPerDay(Session session, String provisionerName) {
        Map<LocalDate, Long> issuedCertificatesPerDay = new HashMap<>();

        Query query = session.createQuery(
                "SELECT cast(o.created as date), COUNT(o) " +
                        "FROM ACMEOrder o " +
                        "WHERE o.certificatePem IS NOT NULL " +
                        (provisionerName != null ? "AND o.account.provisioner = :provisionerName " : "") +
                        "GROUP BY cast(o.created as date)",
                Object[].class
        );

        if(provisionerName != null){
            query.setParameter("provisionerName", provisionerName);
        }


        List<Object[]> results = query.getResultList();

        for (Object[] result : results) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            Long count = (Long) result[1];
            issuedCertificatesPerDay.put(date, count);
        }

        return issuedCertificatesPerDay;
    }

    public static long countACMEAccountsByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(a) FROM ACMEAccount a WHERE a.provisioner = :provisionerName", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countIssuedCertificatesByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NOT NULL", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countRevokedCertificatesByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NOT NULL AND o.revokeStatusCode IS NOT NULL AND o.revokeTimestamp IS NOT NULL", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;

    }

    public static long countCertificatesWaitingForIssueByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NULL AND o.certificateCSR IS NOT NULL", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countGlobalIssuedCertificates(Session session) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.certificatePem IS NOT NULL", Long.class)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countGlobalRevokedCertificates(Session session) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.certificatePem IS NOT NULL AND o.revokeStatusCode IS NOT NULL AND o.revokeTimestamp IS NOT NULL", Long.class)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countGlobalActiveACMEAccounts(Session session) {
        Long count = session.createQuery(
                        "SELECT COUNT(a) FROM ACMEAccount a WHERE a.deactivated = false", Long.class)
                .getSingleResult();
        return count != null ? count : 0;
    }

}
