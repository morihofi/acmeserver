package de.morihofi.acmeserver.config;

import java.io.Serializable;

public class Ports implements Serializable {
    private int http;

    private int https;

    public int getHttp() {
        return this.http;
    }

    public void setHttp(int http) {
        this.http = http;
    }

    public int getHttps() {
        return this.https;
    }

    public void setHttps(int https) {
        this.https = https;
    }
}
