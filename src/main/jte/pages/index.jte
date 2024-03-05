@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ProvisionerResponse
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
@template.layout.navigation(
localizer = localizer,
context = context,
)

<main class="container">

    <!-- Greeting -->
    <div class="bg-body-tertiary p-5 rounded" style="padding-top: 4.5rem;">
        <h1>${localizer.lookup("web.index.title")}</h1>
        ${localizer.lookup("web.index.intro")}<br/>
        <br/>
        <a href="/ca.crt" class="btn btn-primary btn-lg">
            <i class="fa-solid fa-download"></i>&nbsp;
            ${localizer.lookup("web.index.btn.downloadCA")}
        </a>
    </div>

    <br/>
    @if(cryptoStoreManager.isFirstRun())
        <div style="padding: 20px; border: 3px solid yellow;">
            <h2>🔑 Existing CA Certificate Upload</h2>
            <p>If you have already configured a CA certificate and want to use it within ACME Server, please use the
                button
                below
                to upload the new CA certificate.</p>
            <form action="/admin/ca-upload" method="post" enctype="multipart/form-data">
                <label for="caCertificate">CA Certificate</label>
                <input type="file" id="caCertificate" name="caCertificate" accept=".crt,.cer" required>

                <label for="caCertificateKey">CA Certificate Key</label>
                <input type="file" id="caCertificateKey" name="caCertificateKey" accept=".pem,.key" required>
                <br>
                <button type="submit">Upload CA Certificate</button>
            </form>
        </div>
    @endif

    <!-- Provisioners -->
    <h2 class="mb-3">${localizer.lookup("web.index.provisioners.title")}</h2>
    <p class="mb-4">${localizer.lookup("web.index.provisioners.intro")}</p>
    <div id="provisioner-container" class="row g-4 gap-3 mb-4">
        @for(ProvisionerResponse provisioner : serverInfoResponse.getProvisioners())
            <div class="col-md-6 col-lg-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title">${provisioner.getName()}</h5>
                        <a href="/acme/${provisioner.getName()}/directory" class="btn btn-primary mt-auto">
                            <i class="fas fa-link"></i> ${localizer.lookup("web.index.provisioners.list.goToDirectory", "/acme/" + provisioner.getName() + "/directory")}
                        </a>
                        <a href="/gethttpsforfree/?provisioner=${provisioner.getName()}" class="btn btn-secondary mt-2">
                            <i class="fas fa-globe"></i> ${localizer.lookup("web.index.provisioners.list.goToGetHttpsForFree")}
                        </a>
                    </div>
                </div>
            </div>
        @endfor
    </div>


    <!-- Server Info -->
    <h2>${localizer.lookup("web.core.component.serverInfo.title")}</h2>
    @template.components.serverInfo(serverInfoResponse = serverInfoResponse, localizer = localizer)


</main>
@template.layout.footer()
</body>
</html>
