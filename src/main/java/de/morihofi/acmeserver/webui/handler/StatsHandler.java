package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.WebUI;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsHandler implements Handler {
    private CryptoStoreManager cryptoStoreManager;

    public StatsHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {


        List<ACMEOrder> allOrders = Database.getAllACMEOrdersWithState(AcmeOrderState.IDLE);
        List<ACMEAccount> allAccounts = Database.getAllAccounts();

        List<StatisticItem> statisticItemsAllProvisioners = getGlobalStatisticItems(allOrders, allAccounts);
        List<ProvisionerStatistic> provisionerStatistics = getProvisionerStatistics(allOrders, allAccounts);


        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));
        params.put("statisticsAll", statisticItemsAllProvisioners);
        params.put("statisticsProvisioner", provisionerStatistics);


        context.render("pages/stats.jte", params);
    }

    /**
     * Statistics for all provisioners
     *
     * @param allOrders
     * @param allAccounts
     * @return
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


    public static class StatisticItem {
        private final int number;
        private final String translationKey;

        public StatisticItem(int number, String translationKey) {
            this.number = number;
            this.translationKey = translationKey;
        }

        public int getNumber() {
            return number;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public static class ProvisionerStatistic {
        private final String provisionerName;
        private final List<StatisticItem> stats;

        public ProvisionerStatistic(String provisionerName, List<StatisticItem> stats) {
            this.provisionerName = provisionerName;
            this.stats = stats;
        }

        public String getProvisionerName() {
            return provisionerName;
        }

        public List<StatisticItem> getStats() {
            return stats;
        }
    }
}