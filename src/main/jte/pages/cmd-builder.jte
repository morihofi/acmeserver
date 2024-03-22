@import de.morihofi.acmeserver.certificate.provisioners.Provisioner
@import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import io.javalin.http.Context

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context
@param String provisionerName = null
@param String emailAddress = null
@param String domain = null
@param String directoryUrl = null

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
    <h1>${localizer.lookup("web.cmdBuilder.title")}</h1>
    <p class="fs-6">
        ${localizer.lookup("web.cmdBuilder.intro")}
    </p>

    <form method="get" action="${context.path()}">
        <div class="row g-3">
            <div class="col-md-6">
                <label for="emailInput" class="form-label">${localizer.lookup("web.cmdBuilder.input.email")}</label>
                <input type="email" class="form-control" id="emailInput" name="email" value="${emailAddress}" placeholder="info@example.com" required>
            </div>
            <div class="col-md-6">
                <label for="domainInput" class="form-label">${localizer.lookup("web.cmdBuilder.input.domain")}</label>
                <input type="text" class="form-control" id="domainInput" name="domain" value="${domain}" placeholder="example.com" required>
            </div>
            <div class="col-12">
                <label for="select-provisioner" class="form-label">${localizer.lookup("web.cmdBuilder.input.provisioner")}</label>
                <select class="form-select" id="select-provisioner" name="provisioner" required>
                    <option value="">${localizer.lookup("web.cmdBuilder.input.provisioner.default")}</option>
                    @for(Provisioner provisioner : cryptoStoreManager.getProvisioners())
                        <option value="${provisioner.getProvisionerName()}" selected="${provisioner.getProvisionerName().equals(provisionerName)}">${provisioner.getProvisionerName()}</option>
                    @endfor
                </select>
            </div>
        </div>
        <div class="text-center mt-4">
            <button type="submit" class="btn btn-primary">${localizer.lookup("web.cmdBuilder.submit")}</button>
        </div>
    </form>


    @if(provisionerName != null && emailAddress != null && domain != null)
        <div class="bg-light p-4 my-4 rounded-3 shadow-sm">
            <h2 class="h4">${localizer.lookup("web.cmdBuilder.clients.title")}</h2>
            <p>${localizer.lookup("web.cmdBuilder.clients.intro")}</p>

            <!-- Certbot Section -->
            <div class="bg-white p-3 my-3 rounded-3 shadow-sm ">
                <h3 class="mt-3">Certbot</h3>
                <p>${localizer.lookup("web.cmdBuilder.clients.certbot.intro")}</p>
                <pre class="text-black"><code>certbot -n --nginx -d ${domain} --server ${directoryUrl} --agree-tos --email ${emailAddress}</code></pre>
            </div>

            <!-- acme.sh Section -->
            <div class="bg-white p-3 rounded-3 shadow-sm ">
                <h3 class="mt-3">acme.sh</h3>
                <p>${localizer.lookup("web.cmdBuilder.clients.acmeSh.intro")}</p>
                <!-- Registration command -->
                <pre class="text-black"><code>acme.sh --register-account --server ${directoryUrl}</code></pre>
                <!-- Issue command -->
                <pre class="text-black"><code>acme.sh --issue --server ${directoryUrl} -d ${domain} --standalone</code></pre>
            </div>

            <!-- Win-ACME Section -->
            <div class="bg-white p-3 my-3 rounded-3 shadow-sm">
                <h3 class="mt-3">Win-ACME</h3>
                <p>${localizer.lookup("web.cmdBuilder.clients.winAcme.intro")}</p>
                <pre class="text-black"><code>wacs --target manual --host ${domain} --validation selfhosting --store pemfiles --pemfilespath . --baseuri ${directoryUrl} --emailaddress ${emailAddress} --accepttos</code></pre>
            </div>

        </div>
    @endif



</main>
@template.layout.footer(
serverInfoResponse = serverInfoResponse,
localizer = localizer
)
</body>
</html>