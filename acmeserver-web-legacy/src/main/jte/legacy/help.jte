@param String baseUrl

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Help</title>
    <link rel="stylesheet" type="text/css" href="static/style.css">
</head>
<body>
<div id="wrapper">
    @template.legacy.components.menu()
    <h1>Help</h1>
    <p>This guide explains how to install the root certificate in common browsers.</p>
    <h2>Internet Explorer 8 on Windows XP</h2>
    <ol>
        <li>Open "Internet Options" from the Control Panel.</li>
        <li>Select the "Content" tab and click "Certificates".</li>
        <li>Choose "Import" and select the downloaded <strong>DER</strong> file.</li>
        <li>Place the certificate in the "Trusted Root Certification Authorities" store and finish the wizard.</li>
    </ol>
    <h2>Firefox</h2>
    <ol>
        <li>Open the settings and search for "Certificates".</li>
        <li>Click "View Certificates" and then "Import".</li>
        <li>Select the <strong>PEM</strong> file and trust it to identify websites.</li>
    </ol>
    <p>Google Chrome and Microsoft Edge use the Windows certificate store,
        so once you have imported the certificate in Internet Explorer it is available for these browsers as well.</p>

    @template.legacy.components.footer()
</div>
</body>
</html>
