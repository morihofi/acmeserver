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
package de.morihofi.acmeserver.api.provisioner.statistics;

import de.morihofi.acmeserver.api.provisioner.statistics.responses.ProvisionerStatisticResponse;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerStatistics;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

public class ProvisionerGlobalStatisticHandler implements Handler {

    private final CryptoStoreManager cryptoStoreManager;

    public ProvisionerGlobalStatisticHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        ProvisionerStatisticResponse globalStats = new ProvisionerStatisticResponse();
        globalStats.setName(null);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {


            globalStats.setAcmeAccounts(ProvisionerStatistics.countGlobalActiveACMEAccounts(session));
            globalStats.setCertificatesIssued(ProvisionerStatistics.countGlobalIssuedCertificates(session));
            globalStats.setCertificatesRevoked(ProvisionerStatistics.countGlobalRevokedCertificates(session));
            globalStats.setCertificatesIssueWaiting(ProvisionerStatistics.countGlobalCertificatesWaiting(session));

        }

        context.json(globalStats);
    }
}
