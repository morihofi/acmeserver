@import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ServerInfoResponse
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
    <meta charset="utf-8"/>
    <title>ACME Server</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
        }
        .navbar, .footer {
            background-color: #333;
            color: white;
            padding: 10px;
            text-align: center;
        }

        .container {
            padding: 15px;
        }
        a {
            color: #007bff;
            text-decoration: none;
        }
        .alert {
            background-color: #ffcc00;
            padding: 10px;
            margin-bottom: 15px;
        }
        .btn {
            background-color: #007bff;
            color: white;
            padding: 10px;
            text-decoration: none;
            display: inline-block;
            margin-right: 5px;
        }
        .btn:hover {
            background-color: #0056b3;
        }
    </style>
</head>
<body>

<div class="navbar">
    ${localizer.lookup("app.name")}
</div>

<div class="container">

    @if(serverInfoResponse.getMetadataInfo().getVersion().endsWith("-SNAPSHOT"))
        <div class="alert">
            <b>${localizer.lookup("web.core.warnSnapshot.title")}</b>&nbsp;${localizer.lookup("web.core.warnSnapshot.content", localizer.lookup("app.name"))}
        </div>
    @endif

    <h1>${localizer.lookup("app.name")}</h1>
    <p>${localizer.lookup("web.index.legacy.intro")}</p>

    <a href="/ca.crt" class="btn">
        ${localizer.lookup("web.index.btn.downloadCA")}
    </a>

    <ul>
        <li><a href="/ca.der"><img src="./images/download.png" />&nbsp;${localizer.lookup("web.index.btn.downloadCA.der")}</a></li>
        <li><a href="/ca.pem"><img src="./images/download.png" />&nbsp;${localizer.lookup("web.index.btn.downloadCA.pem")}</a></li>
        <li><a href="/ca.cab"><img src="./images/download.png" />&nbsp;${localizer.lookup("web.index.btn.downloadCA.cab")}</a></li>
    </ul>


</div>

</body>
</html>
