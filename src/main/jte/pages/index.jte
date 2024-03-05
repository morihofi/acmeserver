@import de.morihofi.acmeserver.certificate.acme.api.Provisioner
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.ServerInfoEndpoint
@import java.util.List
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ProvisionerResponse
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <link rel="stylesheet" href="css/water.css"/>
    <title>${localizer.lookup("app.name")}</title>
</head>
<body>
<h1>${localizer.lookup("web.index.title")}</h1>
${localizer.lookup("web.index.intro")}<br/>
<br/>
<a href="/ca.crt" class="button">
    <button type="button">${localizer.lookup("web.index.btn.downloadCA")}</button>
</a>
<br/>
@if(cryptoStoreManager.isFirstRun())
    <div style="padding: 20px; border: 3px solid yellow;">
        <h2>🔑 Existing CA Certificate Upload</h2>
        <p>If you have already configured a CA certificate and want to use it within ACME Server, please use the button
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

<h2>${localizer.lookup("web.index.provisioners.title")}</h2>
${localizer.lookup("web.index.provisioners.intro")}
<br/>
<div id="provisioner-container">
    <ul>
        @for(ProvisionerResponse provisioner : serverInfoResponse.getProvisioners())
            <li>
                ${provisioner.getName()}
                <ul>
                    <li>➡️
                        <a href="/${provisioner.getName()}/directory">${localizer.lookup("web.index.provisioners.list.goToDirectory", provisioner.getName())}</a>
                    </li>
                    <li>➡️
                        <a href="/gethttpsforfree/?provisioner=${provisioner.getName()}">${localizer.lookup("web.index.provisioners.list.goToGetHttpsForFree")}</a>
                    </li>
                </ul>
            </li>
        @endfor
    </ul>
</div>

<hr/>
<h2>${localizer.lookup("web.core.serverInfo.title")}</h2>
<b>${localizer.lookup("web.core.serverInfo.version")} </b><label>${serverInfoResponse.getMetadataInfo().getVersion()}</label><br/>
<b>${localizer.lookup("web.core.serverInfo.buildTime")} </b><label>${serverInfoResponse.getMetadataInfo().getBuildTime()}</label><br/>
<b>${localizer.lookup("web.core.serverInfo.gitCommit")} </b><label>${serverInfoResponse.getMetadataInfo().getGitCommit()}</label><br/>
<b>${localizer.lookup("web.core.serverInfo.jvmVersion")} </b><label>${serverInfoResponse.getMetadataInfo().getJavaVersion()}</label><br/>
<b>${localizer.lookup("web.core.serverInfo.os")} </b><label>${serverInfoResponse.getMetadataInfo().getOperatingSystem()}</label><br/>
</body>
</html>
