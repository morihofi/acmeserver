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
    <div class="d-flex flex-column flex-shrink-0 p-3 text-bg-dark vh-100" style="width: 280px;">
        <a href="/" class="d-flex align-items-center mb-3 mb-md-0 me-md-auto text-white text-decoration-none">
            <span class="fs-4">ACME Server</span>
        </a>
        <hr>
        <ul class="nav nav-pills flex-column mb-auto">
            <li class="nav-item">
                <a href="#" class="nav-link active" aria-current="page">
                    <i class="me-2 fa-solid fa-home"></i>
                    Dashboard
                </a>
            </li>
            <li>
                <a href="#" class="nav-link text-white">
                    <i class="me-2 fa-solid fa-shield-halved"></i>
                    Security overview
                </a>
            </li>
            <li>
                <a href="#" class="nav-link text-white">
                    <i class="me-2 fa-solid fa-certificate"></i>
                    Issued certificates
                </a>
            </li>
            <li>
                <a href="#" class="nav-link text-white">
                    <i class="me-2 fa-solid fa-book"></i>
                    Logs
                </a>
            </li>
            <li>
                <a href="#" class="nav-link text-white">
                    <i class="me-2 fa-solid fa-wrench"></i>
                    Configuration
                </a>
            </li>
        </ul>
        <hr>
        <div class="dropdown">
            <a href="#" class="d-flex align-items-center text-white text-decoration-none dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                <img src="https://github.com/mdo.png" alt="" width="32" height="32" class="rounded-circle me-2">
                <strong>Administrator</strong>
            </a>
            <ul class="dropdown-menu dropdown-menu-dark text-small shadow" style="">
                <li><a class="dropdown-item" href="#"><i class="me-2 fa-solid fa-sliders"></i> Settings</a></li>
                <li><hr class="dropdown-divider"></li>
                <li><a class="dropdown-item" href="#"><i class="me-2 fa-solid fa-arrow-right-from-bracket"></i> Sign out</a></li>
            </ul>
        </div>
    </div>

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