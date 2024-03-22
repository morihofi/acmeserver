package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.WebUI;
import io.javalin.http.Context;
import io.javalin.http.Handler;
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


        List<ACMEOrder> allOrders = Database.getAllACMEOrdersWithState(AcmeOrderState.IDLE);
        List<ACMEAccount> allAccounts = Database.getAllAccounts();

        List<StatisticItem> statisticItemsAllProvisioners = getGlobalStatisticItems(allOrders, allAccounts);
        List<ProvisionerStatistic> provisionerStatistics = getProvisionerStatistics(allOrders, allAccounts);
        List<StatisticItem> statisticsDatabase = getDatabaseStatistics();


        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));
        params.put("statisticsAll", statisticItemsAllProvisioners);
        params.put("statisticsProvisioner", provisionerStatistics);
        params.put("statisticsDatabase", statisticsDatabase);


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
     * @param allOrders   The list of all ACME orders.
     * @param allAccounts The list of all ACME accounts.
     * @return A list of {@link StatisticItem} representing the global statistics.
     */
    @NotNull
    private List<StatisticItem> getGlobalStatisticItems(List<ACMEOrder> allOrders, List<ACMEAccount> allAccounts) {
        List<StatisticItem> statisticItemsAllProvisioners = new ArrayList<>();
        // number of provisioners
        statisticItemsAllProvisioners.add(new StatisticItem(cryptoStoreManager.getProvisioners().size(), "web.stats.name.provisioners"));

        // issued Certificates
        statisticItemsAllProvisioners.add(new StatisticItem(
                allOrders.stream()
                        .filter(acmeOrder -> acmeOrder.getCertificatePem() != null)
                        .toList()
                        .size(),
                "web.stats.name.certificatesIssued"));

        // revoked Certificates
        statisticItemsAllProvisioners.add(new StatisticItem(
                allOrders.stream()
                        .filter(acmeOrder -> acmeOrder.getCertificatePem() != null && acmeOrder.getRevokeStatusCode() != null && acmeOrder.getRevokeTimestamp() != null)
                        .toList()
                        .size(),
                "web.stats.name.certificatesRevoked"));

        // (non-deactivated) ACME Accounts
        statisticItemsAllProvisioners.add(new StatisticItem(
                allAccounts.stream()
                        .filter(account -> !account.getDeactivated())
                        .toList()
                        .size(),
                "web.stats.name.acmeAccounts"));

        return statisticItemsAllProvisioners;
    }


    /**
     * Gathers statistical items per provisioner, including the number of ACME accounts, issued and
     * revoked certificates, and certificates waiting to be issued for each provisioner.
     *
     * @param allOrders   The list of all ACME orders.
     * @param allAccounts The list of all ACME accounts.
     * @return A list of {@link ProvisionerStatistic} representing the statistics for each provisioner.
     */
    private List<ProvisionerStatistic> getProvisionerStatistics(List<ACMEOrder> allOrders, List<ACMEAccount> allAccounts) {
        //Statistics per provisioner
        List<ProvisionerStatistic> provisionerStatistics = new ArrayList<>();

        for (Provisioner provisioner : cryptoStoreManager.getProvisioners()) {
            String provisionerName = provisioner.getProvisionerName();
            List<StatisticItem> statisticItemsOfProvisioner = new ArrayList<>();

            // issued Certificates
            statisticItemsOfProvisioner.add(new StatisticItem(allAccounts
                    .stream()
                    .filter(acmeAccount -> acmeAccount.getProvisioner().equals(provisionerName))
                    .toList()
                    .size(),
                    "web.stats.name.acmeAccounts"));

            // issued Certificates
            statisticItemsOfProvisioner.add(new StatisticItem(
                    allOrders.stream()
                            .filter(acmeOrder -> acmeOrder.getAccount().getProvisioner().equals(provisionerName) && acmeOrder.getCertificatePem() != null)
                            .toList()
                            .size(),
                    "web.stats.name.certificatesIssued"));

            // revoked Certificates
            statisticItemsOfProvisioner.add(new StatisticItem(
                    allOrders.stream()
                            .filter(acmeOrder -> acmeOrder.getAccount().getProvisioner().equals(provisionerName) && acmeOrder.getCertificatePem() != null && acmeOrder.getRevokeStatusCode() != null && acmeOrder.getRevokeTimestamp() != null)
                            .toList()
                            .size(),
                    "web.stats.name.certificatesRevoked"));

            // Certificates waiting to be issued
            statisticItemsOfProvisioner.add(new StatisticItem(
                    allOrders.stream()
                            .filter(acmeOrder -> acmeOrder.getAccount().getProvisioner().equals(provisionerName) && acmeOrder.getCertificatePem() == null && acmeOrder.getCertificateCSR() != null)
                            .toList()
                            .size(),
                    "web.stats.name.certificatesIssueWaiting"));

            provisionerStatistics.add(new ProvisionerStatistic(provisionerName, statisticItemsOfProvisioner));
        }

        return provisionerStatistics;
    }


    /**
     * Represents a single statistical item with a number and a translation key for localization.
     */
    public static class StatisticItem {
        private final long number;
        private final String translationKey;

        /**
         * Constructs a new {@link StatisticItem}.
         *
         * @param number         The numeric value of the statistic.
         * @param translationKey The key used for localization of the statistic's description.
         */
        public StatisticItem(long number, String translationKey) {
            this.number = number;
            this.translationKey = translationKey;
        }

        /**
         * @return The numeric value of the statistic.
         */
        public long getNumber() {
            return number;
        }

        /**
         * @return The localization key for the statistic's description.
         */
        public String getTranslationKey() {
            return translationKey;
        }
    }

    /**
     * Represents a collection of statistical data related to a specific provisioner within the system.
     * This class encapsulates both the name of the provisioner and a list of statistics that describe
     * various aspects of its operation, such as the number of issued and revoked certificates, and
     * accounts associated with it. Each statistic is represented by a {@link StatisticItem} object.
     */
    public static class ProvisionerStatistic {
        /**
         * The name of the provisioner to which these statistics pertain.
         */
        private final String provisionerName;

        /**
         * A list of {@link StatisticItem} objects, each representing a different statistical metric
         * associated with the provisioner.
         */
        private final List<StatisticItem> stats;

        /**
         * Constructs a new {@code ProvisionerStatistic} with the specified name and list of statistical items.
         *
         * @param provisionerName the name of the provisioner.
         * @param stats           a list of {@link StatisticItem} objects representing the statistics for the provisioner.
         */
        public ProvisionerStatistic(String provisionerName, List<StatisticItem> stats) {
            this.provisionerName = provisionerName;
            this.stats = stats;
        }

        /**
         * Returns the name of the provisioner.
         *
         * @return the name of the provisioner.
         */
        public String getProvisionerName() {
            return provisionerName;
        }

        /**
         * Returns the list of statistical items associated with the provisioner.
         *
         * @return a list of {@link StatisticItem} objects representing the provisioner's statistics.
         */
        public List<StatisticItem> getStats() {
            return stats;
        }
    }

}
