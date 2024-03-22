package de.morihofi.acmeserver.certificate.api.statistics;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerStatistics;
import de.morihofi.acmeserver.database.HibernateUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

public class ApiStatsProvisionerCertificatesIssued implements Handler {

    private final static Gson gson = new Gson();
    @Override
    public void handle(@NotNull Context context) {
        context.contentType("application/json");

        String provisionerName = context.queryParam("provisioner");

        try(Session session = HibernateUtil.getSessionFactory().openSession()) {
            context.result(gson.toJson(ProvisionerStatistics.getCertificatesIssuedPerDay(session, provisionerName)));
        }

    }

}
