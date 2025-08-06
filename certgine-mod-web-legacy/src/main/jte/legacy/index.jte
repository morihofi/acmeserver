@import de.morihofi.certgine.ui.frontend.legacy.type.CaEntry
@import java.util.List

@param String baseUrl
@param List<CaEntry> cas
@param boolean isWindowsMobile
@param boolean isWindowsDesktop
@param boolean isAndroid

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Certificate Authority Downloads</title>
    <link rel="stylesheet" type="text/css" href="static/style.css">
</head>
<body>
<div id="wrapper">
    @template.legacy.components.menu()

    <h1>Root Certificate Authorities</h1>
    <p>
        Download the root certificate authorities for your platform. If you are unsure which one to use, please refer to the documentation of your operating system, application or device.
        However, we try to provide the most common ones here with a recommendation for your current platform.
    </p>

    <table class="ca-table">
        <thead>
        <tr class="ca-header">
            <th>&nbsp;</th>
            <th>Certificate Authority</th>
            <th>Downloads</th>
        </tr>
        </thead>
        <tbody>
        @for (CaEntry ca : cas)
            <tr class="ca-entry${ca.isPrimary() ? " primary" : ""}">
                <td>
                    <img src="static/certificate.png" alt="Certificate" width="16" height="16">
                </td>
                <td>
                    <strong>${ca.getName()}</strong><br />
                    <small>${ca.getDescription()}</small><br /><br />
                    <span class="ca-info">
                        <a href="ca?id=${ca.getId()}">Show details</a>
                    </span>
                </td>
                <td>
                    <img src="static/download.png" alt="Download" width="16" height="16">
                    <a href="${ca.getPemPath()}">PEM</a>
                    @if (isAndroid)
                        <span class="recommend">
                        <img src="static/info.png" alt="★" width="16" height="16"> Recommended for your platform
                    </span>
                    @endif
                    <br>

                    <img src="static/download.png" alt="Download" width="16" height="16">
                    <a href="${ca.getDerPath()}">DER</a>
                    @if (isWindowsDesktop)
                        <span class="recommend">
                        <img src="static/info.png" alt="★" width="16" height="16"> Recommended for your platform
                    </span>
                    @endif
                    <br>

                    <img src="static/download.png" alt="Download" width="16" height="16">
                    <a href="${ca.getCabPath()}">CAB</a>
                    @if (isWindowsMobile)
                        <span class="recommend">
                        <img src="static/info.png" alt="★" width="16" height="16"> Recommended for your platform
                    </span>
                    @endif
                </td>
            </tr>
        @endfor
        </tbody>
    </table>



    @template.legacy.components.footer()
</div>
</body>
</html>
