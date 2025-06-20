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
    <h1>${ca.getName()}</h1>
    @if(ca.isEcdsa())
        <div class="warning">Internet Explorer might not support ECDSA root certificates.</div>
    @endif
    <table class="ca-table">
        <tr class="ca-header"><th>Download</th><th>&nbsp;</th></tr>
        <tr><td><a href="${ca.getPemPath()}">PEM</a></td><td>PEM format</td></tr>
        <tr><td><a href="${ca.getDerPath()}">DER</a></td><td>DER format</td></tr>
        <tr><td><a href="${ca.getCabPath()}">CAB</a></td><td>Windows Mobile CAB</td></tr>
    </table>
    <div id="footer">Powered by <a href="https://github.com/morihofi/acmeserver">morihofi's ACME Server</a> | Icons by Gnome Icon Theme 2.18.0</div>
</div>
</body>
</html>
