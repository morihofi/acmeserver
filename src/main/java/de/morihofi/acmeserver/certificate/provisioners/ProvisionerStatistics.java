/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.provisioners;

import de.morihofi.acmeserver.tools.safety.TypeSafetyHelper;
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

        if (provisionerName != null) {
            query.setParameter("provisionerName", provisionerName);
        }

        List<Object[]> results = TypeSafetyHelper.safeCastToClassOfType(query.getResultList(), Object[].class);

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
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NOT NULL",
                        Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countRevokedCertificatesByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NOT NULL"
                                + " AND o.revokeStatusCode IS NOT NULL AND o.revokeTimestamp IS NOT NULL",
                        Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public static long countCertificatesWaitingForIssueByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NULL AND"
                                + " o.certificateCSR IS NOT NULL",
                        Long.class)
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
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.certificatePem IS NOT NULL AND o.revokeStatusCode IS NOT NULL AND o"
                                + ".revokeTimestamp IS NOT NULL",
                        Long.class)
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
