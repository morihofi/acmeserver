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

package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerStatistics;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.WebUI;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the generation and display of statistical information about ACME orders, accounts, and provisioners. This handler collects data
 * from the database and organizes it into various statistical metrics to be displayed on the stats page.
 */
public class StatsHandler implements Handler {
    private final CryptoStoreManager cryptoStoreManager;

    /**
     * Constructs a new {@link StatsHandler} with a reference to the {@link CryptoStoreManager}.
     *
     * @param cryptoStoreManager The crypto store manager for accessing cryptographic operations and provisioners.
     */
    public StatsHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    /**
     * Handles the incoming request for the statistics page, gathering and preparing all necessary statistical data to be displayed in the
     * web UI.
     *
     * @param context The context of the incoming request.
     */
    @Override
    public void handle(@NotNull Context context) {

        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<StatisticItem> statisticItemsAllProvisioners = getGlobalStatisticItems(session);
            List<ProvisionerStatistic> provisionerStatistics = getProvisionerStatistics(session);
            List<StatisticItem> statisticsDatabase = getDatabaseStatistics();

            params.put("statisticsAll", statisticItemsAllProvisioners);
            params.put("statisticsProvisioner", provisionerStatistics);
            params.put("statisticsDatabase", statisticsDatabase);
        }

        context.render("html5/pages/stats.jte", params);
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
     * Gathers global statistical items, such as total number of provisioners, issued and revoked certificates, and active ACME accounts.
     *
     * @return A list of {@link StatisticItem} representing the global statistics.
     */

    private List<StatisticItem> getGlobalStatisticItems(Session session) {
        List<StatisticItem> statisticItemsAllProvisioners = new ArrayList<>();
        // number of provisioners
        statisticItemsAllProvisioners.add(new StatisticItem(ProvisionerManager.getProvisioners().size(), "web.stats.name.provisioners"));

        // issued Certificates
        statisticItemsAllProvisioners.add(
                new StatisticItem(ProvisionerStatistics.countGlobalIssuedCertificates(session), "web.stats.name.certificatesIssued"));

        // revoked Certificates
        statisticItemsAllProvisioners.add(
                new StatisticItem(ProvisionerStatistics.countGlobalRevokedCertificates(session), "web.stats.name.certificatesRevoked"));

        // (non-deactivated) ACME Accounts
        statisticItemsAllProvisioners.add(
                new StatisticItem(ProvisionerStatistics.countGlobalActiveACMEAccounts(session), "web.stats.name.acmeAccounts"));

        return statisticItemsAllProvisioners;
    }

    /**
     * Gathers statistical items per provisioner, including the number of ACME accounts, issued and revoked certificates, and certificates
     * waiting to be issued for each provisioner.
     *
     * @return A list of {@link ProvisionerStatistic} representing the statistics for each provisioner.
     */
    private List<ProvisionerStatistic> getProvisionerStatistics(Session session) {
        // Statistics per provisioner
        List<ProvisionerStatistic> provisionerStatistics = new ArrayList<>();

        for (Provisioner provisioner : ProvisionerManager.getProvisioners()) {
            String provisionerName = provisioner.getProvisionerName();
            List<StatisticItem> statisticItemsOfProvisioner = new ArrayList<>();

            // issued Certificates
            statisticItemsOfProvisioner.add(
                    new StatisticItem(ProvisionerStatistics.countACMEAccountsByProvisioner(session, provisionerName),
                            "web.stats.name.acmeAccounts"));

            // issued Certificates
            statisticItemsOfProvisioner.add(
                    new StatisticItem(ProvisionerStatistics.countIssuedCertificatesByProvisioner(session, provisionerName),
                            "web.stats.name.certificatesIssued"));

            // revoked Certificates
            statisticItemsOfProvisioner.add(
                    new StatisticItem(ProvisionerStatistics.countRevokedCertificatesByProvisioner(session, provisionerName),
                            "web.stats.name.certificatesRevoked"));

            // Certificates waiting to be issued
            statisticItemsOfProvisioner.add(
                    new StatisticItem(ProvisionerStatistics.countCertificatesWaitingForIssueByProvisioner(session, provisionerName),
                            "web.stats.name.certificatesIssueWaiting"));

            provisionerStatistics.add(new ProvisionerStatistic(provisionerName, statisticItemsOfProvisioner));
        }

        return provisionerStatistics;
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
     * Represents a collection of statistical data related to a specific provisioner within the system. This class encapsulates both the
     * name of the provisioner and a list of statistics that describe various aspects of its operation, such as the number of issued and
     * revoked certificates, and accounts associated with it. Each statistic is represented by a {@link StatisticItem} object.
     *
     * @param provisionerName The name of the provisioner to which these statistics pertain.
     * @param stats           A list of {@link StatisticItem} objects, each representing a different statistical metric associated with the
     *                        provisioner.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
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
        @SuppressFBWarnings("EI_EXPOSE_REP")
        @Override
        public List<StatisticItem> stats() {
            return stats;
        }
    }
}
