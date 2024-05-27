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
package de.morihofi.acmeserver.api;

import de.morihofi.acmeserver.api.responses.ProvisionerStatisticListResponse;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerStatistics;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProvisionerStatisticHandler implements Handler {

    private final CryptoStoreManager cryptoStoreManager;

    public ProvisionerStatisticHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        List<ProvisionerStatisticListResponse> statisticItemsOfProvisioner = new ArrayList<>();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            for (Provisioner provisioner : ProvisionerManager.getProvisioners()) {
                String provisionerName = provisioner.getProvisionerName();

                ProvisionerStatisticListResponse item = new ProvisionerStatisticListResponse();
                item.setName(provisionerName);
                item.setAcmeAccounts(ProvisionerStatistics.countACMEAccountsByProvisioner(session, provisionerName));
                item.setCertificatesIssued(ProvisionerStatistics.countIssuedCertificatesByProvisioner(session, provisionerName));
                item.setCertificatesRevoked(ProvisionerStatistics.countRevokedCertificatesByProvisioner(session, provisionerName));
                item.setCertificatesIssueWaiting(
                        ProvisionerStatistics.countCertificatesWaitingForIssueByProvisioner(session, provisionerName));

                statisticItemsOfProvisioner.add(item);
            }
        }

        context.json(statisticItemsOfProvisioner);
    }
}
