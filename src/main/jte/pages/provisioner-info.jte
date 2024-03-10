@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.compontents.table.TableKey
@import de.morihofi.acmeserver.webui.compontents.table.TableValue
@import io.javalin.http.Context
@import java.util.Map

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context
@param String provisionerName
@param Map<TableKey,TableValue> provisionerTableMap
@param Map<TableKey,TableValue> provisionerIntermediateTableMap

<!DOCTYPE html>
<html>
<head>
    @template.layout.head()

    <title>${localizer.lookup("app.name")}</title>
</head>
<body>
@template.layout.navigation(
localizer = localizer,
context = context,
)

<main class="container">
    <h1>${localizer.lookup("web.provisionerInfo.title", provisionerName)}</h1>
    ${localizer.lookup("web.provisionerInfo.intro")}

    <h2 class="mt-3">${localizer.lookup("web.provisionerInfo.general.title")}</h2>
    @template.components.stringTable(localizer = localizer, table = provisionerTableMap)

    <h2 class="mt-3">${localizer.lookup("web.provisionerInfo.intermediate.title")}</h2>
    @template.components.stringTable(localizer = localizer, table = provisionerIntermediateTableMap)

</main>
@template.layout.footer(
serverInfoResponse = serverInfoResponse
)
</body>
</html>