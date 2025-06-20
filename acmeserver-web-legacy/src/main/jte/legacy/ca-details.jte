@import de.morihofi.acmeserver.ui.frontend.legacy.type.CaEntry
@param String baseUrl
@param CaEntry ca

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>${ca.getName()}</title>
    <link rel="stylesheet" type="text/css" href="static/style.css">
</head>
<body>
<div id="wrapper">
    @template.legacy.components.menu()
    <h1>${ca.getName()}</h1>
    <a class="back-link" href="ca-list"><img src="static/go-previous.png" alt="Back" width="16" height="16"> Back to
        list</a>
    @if(ca.isEcdsa())
        <div class="warning">Internet Explorer might not support ECDSA root certificates.</div>
    @endif
    <h2>Certificate Details</h2>
    <table class="detail-table">
        <tr>
            <th>Common Name (CN)</th>
            <td>${ca.getCommonName()}</td>
        </tr>
        <tr>
            <th>Organization (O)</th>
            <td>${ca.getOrganisation()}</td>
        </tr>
        <tr>
            <th>Organizational Unit (OU)</th>
            <td>${ca.getOrganisationalUnit()}</td>
        </tr>
        <tr>
            <th>Country (C)</th>
            <td>${ca.getCountryCode()}</td>
        </tr>
        <tr>
            <th>Algorithm</th>
            <td>${ca.getAlgorithmDetail()}</td>
        </tr>
        <tr>
            <th>Fingerprint (SHA-256)</th>
            <td>${ca.getSha256Fingerprint()}</td>
        </tr>
        <tr>
            <th>Fingerprint (SHA-1)</th>
            <td>${ca.getSha1Fingerprint()}</td>
        </tr>
    </table>

    <h2>Downloads</h2>
    <table class="download-table">
        <tr>
            <td><img src="static/download.png" alt="Download" width="16" height="16"></td>
            <td><a href="${ca.getPemPath()}">PEM</a></td>
            <td>Text format, common on Unix systems.</td>
        </tr>
        <tr>
            <td><img src="static/download.png" alt="Download" width="16" height="16"></td>
            <td><a href="${ca.getDerPath()}">DER</a></td>
            <td>Binary format, used by Windows and Java.</td>
        </tr>
        <tr>
            <td><img src="static/download.png" alt="Download" width="16" height="16"></td>
            <td><a href="${ca.getCabPath()}">CAB</a></td>
            <td>Package for Windows Mobile.</td>
        </tr>
    </table>
    @template.legacy.components.footer()
</div>
</body>
</html>
