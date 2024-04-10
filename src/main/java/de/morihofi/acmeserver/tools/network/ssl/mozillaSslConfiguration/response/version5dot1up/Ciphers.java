package de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version5dot1up;

import java.util.List;

public class Ciphers {
    private List<String> openssl;
    private List<String> go;
    private List<String> iana;
    private List<String> caddy;

    public List<String> getOpenssl() {
        return openssl;
    }

    public List<String> getGo() {
        return go;
    }

    public List<String> getIana() {
        return iana;
    }

    public List<String> getCaddy() {
        return caddy;
    }
}
