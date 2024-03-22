@import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.database.objects.Users
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import io.javalin.http.Context

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context
@param Users user


<!DOCTYPE html>
<html>
<head>
    @template.layout.head()


    <title>${localizer.lookup("app.name")}</title>
</head>
<body>


<main class="d-flex flex-nowrap">
    @template.layout.mgmtNavigation(localizer = localizer, context = context, user = user)

    Test

</main>

@template.layout.footer(localizer = localizer)

</body>
</html>