@import de.morihofi.acmeserver.certificate.acme.api.Provisioner
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
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
    <p  class="fs-6">
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
        <div class="bg-light p-4 my-4 rounded-3">
            <h2 class="h4">Choose Your Preferred ACME Client</h2>
            <p>The code samples below are just starting points. Adjust the commands according to your needs.</p>

            <!-- Certbot Section -->
            <div class="bg-dark text-white p-3 my-3 rounded-3">
                <h3 class="mt-3">Certbot</h3>
                <p>Certbot is a free, open source software tool for automatically using Let's Encrypt certificates on manually-administered websites to enable HTTPS.</p>
                <pre class="text-white"><code>certbot -n --nginx -d ${domain} --server ${cryptoStoreManager.getProvisionerForName(provisionerName).getApiURL() + "/directory"} --agree-tos --email ${emailAddress}</code></pre>
            </div>

            <!-- acme.sh Section -->
            <div class="bg-dark text-white p-3 rounded-3">
                <h3 class="mt-3">acme.sh</h3>
                <p>acme.sh is an ACME protocol client written in shell that can be used to obtain certificates from an ACME server, designed to be pure POSIX compliant, thus working on a wide range of shell environments and operating systems.</p>
                <!-- Registration command -->
                <pre class="text-white"><code>acme.sh --register-account --server https://acme.example.com/acme/myprovisioner/directory</code></pre>
                <!-- Issue command -->
                <pre class="text-white"><code>acme.sh --issue --server https://acme.example.com/acme/myprovisioner/directory -d ${domain} --standalone</code></pre>
            </div>
        </div>
    @endif



</main>
@template.layout.footer(
serverInfoResponse = serverInfoResponse
)
</body>
</html>