@import de.morihofi.acmeserver.certificate.acme.api.Provisioner
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.ServerInfoEndpoint
@import java.util.List
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ProvisionerResponse
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse

@param ServerInfoResponse serverInfoResponse

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <link rel="stylesheet" href="css/water.css"/>
    <title>ACME Server</title>
</head>
<body>
<h1>🥳 ACME Server is running!</h1>
If you see this 🔒-Icon in your address bar of your browser, everything is
correct configured. Otherwise, you have to import
<a href="/ca.crt">this CA Certificate</a> onto your PC or Browser.<br/>
<br/>

<h2>Available ACME Provisioners</h2>
You can't do much here, but you can view all available provisioners or create a certificate with GetHTTPSForFree<br/>
<div id="provisioner-container">
    <ul>
        @for(ProvisionerResponse provisioner : serverInfoResponse.getProvisioners())
            <li>
                ${provisioner.getName()}
                <ul>
                    <li>➡️ <a href="/${provisioner.getName()}/directory">Go to /${provisioner.getName()}/directory Endpoint</a></li>
                    <li>➡️ <a href="/gethttpsforfree/?provisioner=${provisioner.getName()}">Create a ACME Certificate through GetHTTPSForFree UI</a></li>
                </ul>
            </li>
        @endfor
    </ul>
</div>

<hr/>
<h2>💡 Server Info</h2>
<b>ℹ️ Server Version: </b><label>${serverInfoResponse.getMetadataInfo().getVersion()}</label><br/>
<b>⌚ Build at: </b><label>${serverInfoResponse.getMetadataInfo().getBuildTime()}</label><br/>
<b>📝 Git Commit: </b><label>${serverInfoResponse.getMetadataInfo().getGitCommit()}</label><br/>
<b>☕ Java Version: </b><label>${serverInfoResponse.getMetadataInfo().getJavaVersion()}</label><br/>
<b>🖥️ Operating System: </b><label>${serverInfoResponse.getMetadataInfo().getOperatingSystem()}</label><br/>
</body>
</html>
