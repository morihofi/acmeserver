@param String baseUrl

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>About</title>
    <link rel="stylesheet" type="text/css" href="static/style.css">
</head>
<body>
<div id="wrapper">
    @template.legacy.components.menu()
    <h1>About</h1>
    <p>
        This interface lets legacy or resource-constrained devices download and
        install the required root certificates.
    </p>

    <h2>Why does this legacy section exist?</h2>
    <p>
        ACME Server is designed for home labs and small organisations.
        Many networks still include older hardware that should trust the same
        private certificate authority as modern systems. Enabling TLS on these
        devices is far better than allowing them to send data unencrypted—even
        though such devices often support only earlier TLS versions like
        <abbr title="Transport Layer Security">TLS</abbr>&nbsp;1.0 or 1.1,
        which are <strong>less secure than current standards</strong>.
    </p>
    <p>
        Because many legacy Microsoft-based devices expect certificate bundles
        in a <code>.cab</code> archive, we also offer the CA package in that
        format.
    </p>

    <h2>Important note for Internet&nbsp;Explorer users</h2>

    <p>
        <strong>This page cannot be opened with Internet&nbsp;Explorer (especially IE 8).</strong>
        The browser’s built-in cryptography does not satisfy current TLS requirements,
        and Internet Explorer is officially considered legacy software. Be aware that Internet Explorer is not
        officially supported!
    </p>

    <p>
        Even if you enable support for TLS&nbsp;1.0, remember that this protocol is
        <em>significantly weaker than modern TLS versions</em>; use it only when no
        alternative exists.
    </p>

    <h3>Server-side guidance</h3>
    <p>
        If you absolutely must support legacy clients,
        enable the Mozilla <code>old</code> SSL profile and turn on
        <code>allowLegacyResumption</code>. This lets browsers limited to TLS&nbsp;1.0
        complete the handshake, but it <strong>reduces overall security</strong> and
        is recommended only for homelab or other low-risk environments.
    </p>
    <h3>Client-side guidance (here with IE 8)</h3>
    <p>
        Open <strong>Internet&nbsp;Options &gt; Advanced &gt; Security</strong>,
        uncheck <abbr>SSL</abbr>&nbsp;2.0 and 3.0, and leave only <strong>TLS&nbsp;1.0</strong> enabled.
        Otherwise, the handshake fails with:&nbsp;<code>Failed to read record: Unsupported&nbsp;UNKNOWN(128)</code>&nbsp;This
        occurs because Bouncy Castle supports TLS&nbsp;1.0 and newer but not the
        obsolete SSL protocols.
    </p>

    @template.legacy.components.footer()
</div>
</body>
</html>
