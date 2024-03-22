package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.WebUI;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.hibernate.stat.Statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the generation and display of statistical information about ACME orders, accounts,
 * and provisioners. This handler collects data from the database and organizes it into various
 * statistical metrics to be displayed on the stats page.
 */
public class StatsHandler implements Handler {
    private CryptoStoreManager cryptoStoreManager;

    /**
     * Constructs a new {@link StatsHandler} with a reference to the {@link CryptoStoreManager}.
     *
     * @param cryptoStoreManager The crypto store manager for accessing cryptographic operations and provisioners.
     */
    public StatsHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    /**
     * Handles the incoming request for the statistics page, gathering and preparing all necessary
     * statistical data to be displayed in the web UI.
     *
     * @param context The context of the incoming request.
     * @throws Exception If there's an error processing the request or accessing database resources.
     */
    @Override
    public void handle(@NotNull Context context) throws Exception {

        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));


        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<StatisticItem> statisticItemsAllProvisioners = getGlobalStatisticItems(session);
            List<ProvisionerStatistic> provisionerStatistics = getProvisionerStatistics(session);
            List<StatisticItem> statisticsDatabase = getDatabaseStatistics();


            params.put("statisticsAll", statisticItemsAllProvisioners);
            params.put("statisticsProvisioner", provisionerStatistics);
            params.put("statisticsDatabase", statisticsDatabase);
        }

        context.render("pages/stats.jte", params);
    }

    private List<StatisticItem> getDatabaseStatistics() {
        List<StatisticItem> statisticItems = new ArrayList<>();

        Statistics stats = HibernateUtil.getSessionFactory().getStatistics();

        statisticItems.add(new StatisticItem(stats.getQueryExecutionCount(), "web.stats.database.name.queryExecutionCount"));
        statisticItems.add(new StatisticItem(stats.getQueryCacheHitCount(), "web.stats.database.name.queryCacheHitCount"));
        statisticItems.add(new StatisticItem(stats.getSecondLevelCacheHitCount(), "web.stats.database.name.secondLevelCacheHitCount"));
        statisticItems.add(new StatisticItem(stats.getConnectCount(), "web.stats.database.name.connectCount"));
        statisticItems.add(new StatisticItem(stats.getSessionOpenCount(), "web.stats.database.name.sessionOpenCount"));
        statisticItems.add(new StatisticItem(stats.getEntityFetchCount(), "web.stats.database.name.entityFetchCount"));
        statisticItems.add(new StatisticItem(stats.getEntityInsertCount(), "web.stats.database.name.entityInsertCount"));
        statisticItems.add(new StatisticItem(stats.getEntityUpdateCount(), "web.stats.database.name.entityUpdateCount"));

        return statisticItems;
    }

    /**
     * Gathers global statistical items, such as total number of provisioners, issued and revoked
     * certificates, and active ACME accounts.
     *
     * @return A list of {@link StatisticItem} representing the global statistics.
     */
    @NotNull
    private List<StatisticItem> getGlobalStatisticItems(Session session) {
        List<StatisticItem> statisticItemsAllProvisioners = new ArrayList<>();
        // number of provisioners
        statisticItemsAllProvisioners.add(new StatisticItem(ProvisionerManager.getProvisioners().size(), "web.stats.name.provisioners"));

        // issued Certificates
        statisticItemsAllProvisioners.add(new StatisticItem(countGlobalIssuedCertificates(session), "web.stats.name.certificatesIssued"));

        // revoked Certificates
        statisticItemsAllProvisioners.add(new StatisticItem(countGlobalRevokedCertificates(session), "web.stats.name.certificatesRevoked"));

        // (non-deactivated) ACME Accounts
        statisticItemsAllProvisioners.add(new StatisticItem(countGlobalActiveACMEAccounts(session), "web.stats.name.acmeAccounts"));

        return statisticItemsAllProvisioners;
    }


    /**
     * Gathers statistical items per provisioner, including the number of ACME accounts, issued and
     * revoked certificates, and certificates waiting to be issued for each provisioner.
     *
     * @return A list of {@link ProvisionerStatistic} representing the statistics for each provisioner.
     */
    private List<ProvisionerStatistic> getProvisionerStatistics(Session session) {
        //Statistics per provisioner
        List<ProvisionerStatistic> provisionerStatistics = new ArrayList<>();


        for (Provisioner provisioner : ProvisionerManager.getProvisioners()) {
            String provisionerName = provisioner.getProvisionerName();
            List<StatisticItem> statisticItemsOfProvisioner = new ArrayList<>();

            // issued Certificates
            statisticItemsOfProvisioner.add(new StatisticItem(countACMEAccountsByProvisioner(session, provisionerName), "web.stats.name.acmeAccounts"));

            // issued Certificates
            statisticItemsOfProvisioner.add(new StatisticItem(countIssuedCertificatesByProvisioner(session, provisionerName), "web.stats.name.certificatesIssued"));

            // revoked Certificates
            statisticItemsOfProvisioner.add(new StatisticItem(countRevokedCertificatesByProvisioner(session, provisionerName), "web.stats.name.certificatesRevoked"));

            // Certificates waiting to be issued
            statisticItemsOfProvisioner.add(new StatisticItem(countCertificatesWaitingForIssueByProvisioner(session, provisionerName), "web.stats.name.certificatesIssueWaiting"));

            provisionerStatistics.add(new ProvisionerStatistic(provisionerName, statisticItemsOfProvisioner));
        }

        return provisionerStatistics;
    }

    public long countACMEAccountsByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(a) FROM ACMEAccount a WHERE a.provisioner = :provisionerName", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public long countIssuedCertificatesByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NOT NULL", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public long countRevokedCertificatesByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NOT NULL AND o.revokeStatusCode IS NOT NULL AND o.revokeTimestamp IS NOT NULL", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;

    }

    public long countCertificatesWaitingForIssueByProvisioner(Session session, String provisionerName) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.account.provisioner = :provisionerName AND o.certificatePem IS NULL AND o.certificateCSR IS NOT NULL", Long.class)
                .setParameter("provisionerName", provisionerName)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public long countGlobalIssuedCertificates(Session session) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.certificatePem IS NOT NULL", Long.class)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public long countGlobalRevokedCertificates(Session session) {
        Long count = session.createQuery(
                        "SELECT COUNT(o) FROM ACMEOrder o WHERE o.certificatePem IS NOT NULL AND o.revokeStatusCode IS NOT NULL AND o.revokeTimestamp IS NOT NULL", Long.class)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public long countGlobalActiveACMEAccounts(Session session) {
        Long count = session.createQuery(
                        "SELECT COUNT(a) FROM ACMEAccount a WHERE a.deactivated = false", Long.class)
                .getSingleResult();
        return count != null ? count : 0;
    }


    /**
     * Represents a single statistical item with a number and a translation key for localization.
     */
    public record StatisticItem(long number, String translationKey) {
        /**
         * Constructs a new {@link StatisticItem}.
         *
         * @param number         The numeric value of the statistic.
         * @param translationKey The key used for localization of the statistic's description.
         */
        public StatisticItem {
        }

        /**
         * @return The numeric value of the statistic.
         */
        @Override
        public long number() {
            return number;
        }

        /**
         * @return The localization key for the statistic's description.
         */
        @Override
        public String translationKey() {
            return translationKey;
        }
    }

    /**
     * Represents a collection of statistical data related to a specific provisioner within the system.
     * This class encapsulates both the name of the provisioner and a list of statistics that describe
     * various aspects of its operation, such as the number of issued and revoked certificates, and
     * accounts associated with it. Each statistic is represented by a {@link StatisticItem} object.
     *
     * @param provisionerName The name of the provisioner to which these statistics pertain.
     * @param stats           A list of {@link StatisticItem} objects, each representing a different statistical metric
     *                        associated with the provisioner.
     */
    public record ProvisionerStatistic(String provisionerName, List<StatisticItem> stats) {
        /**
         * Constructs a new {@code ProvisionerStatistic} with the specified name and list of statistical items.
         *
         * @param provisionerName the name of the provisioner.
         * @param stats           a list of {@link StatisticItem} objects representing the statistics for the provisioner.
         */
        public ProvisionerStatistic {
        }

        /**
         * Returns the name of the provisioner.
         *
         * @return the name of the provisioner.
         */
        @Override
        public String provisionerName() {
            return provisionerName;
        }

        /**
         * Returns the list of statistical items associated with the provisioner.
         *
         * @return a list of {@link StatisticItem} objects representing the provisioner's statistics.
         */
        @Override
        public List<StatisticItem> stats() {
            return stats;
        }
    }

}
