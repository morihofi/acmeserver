@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.handler.StatsHandler
@import io.javalin.http.Context
@import java.util.List

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context
@param List<StatsHandler.StatisticItem> statisticsAll
@param List<StatsHandler.ProvisionerStatistic> statisticsProvisioner


<!DOCTYPE html>
<html>
<head>
    @template.layout.head()

    <title>${localizer.lookup("app.name")}</title>
</head>
<body>
@template.layout.navigation(
localizer = localizer,
context = context
)

<main class="container">
    <h1>${localizer.lookup("web.stats.title")}</h1>
    <p>${localizer.lookup("web.stats.intro")}</p>

    <div class="container mt-3 mb-3">
        <div class="row">
            @for(StatsHandler.StatisticItem item : statisticsAll)
                <div class="col-md-3">
                    <div class="card text-center">
                        <div class="card-body">
                            <h5 class="card-title display-6 fw-bold">${item.getNumber()}</h5>
                            <p class="card-text">${localizer.lookup(item.getTranslationKey())}</p>
                        </div>
                    </div>
                </div>
            @endfor
        </div>
    </div>


    <h2>${localizer.lookup("web.stats.perProvisioner.title")}</h2>
    <p>${localizer.lookup("web.stats.perProvisioner.intro")}</p>

    @for(StatsHandler.ProvisionerStatistic statistic: statisticsProvisioner)
        <div class="card mb-3">
            <a href="/provisioner-info?name=${statistic.getProvisionerName()}" class="position-absolute top-0 end-0 p-2 text-reset">
                <i class="fas fa-info-circle"></i>
            </a>
            <div class="card-header">
                ${statistic.getProvisionerName()}
            </div>
            <div class="card-body">
                <div class="container mt-3 mb-3">
                    <div class="row">
                        @for(StatsHandler.StatisticItem item : statistic.getStats())
                            <div class="col-md-3">
                                <div class="card text-center">
                                    <div class="card-body">
                                        <h5 class="card-title display-6 fw-bold">${item.getNumber()}</h5>
                                        <p class="card-text">${localizer.lookup(item.getTranslationKey())}</p>
                                    </div>
                                </div>
                            </div>
                        @endfor
                    </div>
                </div>
            </div>
        </div>
    @endfor


</main>
@template.layout.footer(
serverInfoResponse = serverInfoResponse,
localizer = localizer
)
</body>
</html>