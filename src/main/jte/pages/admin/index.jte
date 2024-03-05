@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import io.javalin.http.Context

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context


<!DOCTYPE html>
<html>
<head>
    @template.layout.head()


    <title>${localizer.lookup("app.name")}</title>
</head>
<body>


<main class="d-flex flex-nowrap">
    @template.layout.adminNavigation(localizer = localizer, context = context)

    Test

</main>

@template.layout.footer()
<!-- Bootstrap -->
<script>
/* global bootstrap: false */
(() => {
  'use strict'
  const tooltipTriggerList = Array.from(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  tooltipTriggerList.forEach(tooltipTriggerEl => {
    new bootstrap.Tooltip(tooltipTriggerEl)
  })
})()
</script>

</body>
</html>