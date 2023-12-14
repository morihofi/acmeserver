package de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig;

import java.nio.file.Path;

public class PKCS12KeyStoreConfig implements IKeyStoreConfig{
    private String password;
    private Path path;

    public PKCS12KeyStoreConfig(Path path, String password) {
        this.password = password;
        this.path = path;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
