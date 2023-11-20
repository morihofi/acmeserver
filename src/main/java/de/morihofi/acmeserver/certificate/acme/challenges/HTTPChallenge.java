package de.morihofi.acmeserver.certificate.acme.challenges;

import de.morihofi.acmeserver.Main;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;

public class HTTPChallenge {

    private HTTPChallenge(){

    }

    public static final Logger log = LogManager.getLogger(HTTPChallenge.class);
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().proxy(getHTTPChallengeProxy()).build();

    private static String proxyHost = "";
    private static int proxyPort = 0;
    private static String proxyUser = "";
    private static String proxyPassword = "";

    private static Proxy proxy;


    public static Proxy getHTTPChallengeProxy() {
        Proxy.Type proxyType = switch (Main.appConfig.getProxy().getHttpChallenge().getType()) {
            case "socks" -> Proxy.Type.SOCKS;
            case "http" -> Proxy.Type.HTTP;
            default -> Proxy.Type.DIRECT;
        };

        try {
            proxyPort = Main.appConfig.getProxy().getHttpChallenge().getPort();
            proxyHost = Main.appConfig.getProxy().getHttpChallenge().getHost();


            if (!Main.appConfig.getProxy().getHttpChallenge().getEnabled()) {
                // Set to direct if there is no proxy enabled
                proxyType = Proxy.Type.DIRECT;
                proxy = Proxy.NO_PROXY;
            }else{
                SocketAddress socketAddress = new InetSocketAddress(proxyHost, proxyPort);
                proxy = new Proxy(proxyType, socketAddress);
            }




            if (Main.appConfig.getProxy().getHttpChallenge().getAuthentication().isEnabled()) {
                proxyUser = Main.appConfig.getProxy().getHttpChallenge().getAuthentication().getUsername();
                proxyPassword = Main.appConfig.getProxy().getHttpChallenge().getAuthentication().getPassword();

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(proxyHost) && proxyPort == getRequestingPort()) {
                                return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                        }
                        return null;
                    }
                });

            }


        } catch (Exception ex) {
            log.error("Failed to initialize proxy configuration", ex);
        }


        return proxy;
    }

    private static final String USER_AGENT = "Mozilla/5.0 ACMEServer/" + Main.buildMetadataVersion + " Java/" + System.getProperty("java.version");

    public static boolean check(String expectedAuthTokenValue, String host) {
        boolean passed = false;

        try {

            // avoid creating several instances, should be singleon


            Request request = new Request.Builder()
                    .url("http://" + host + "/.well-known/acme-challenge/" + expectedAuthTokenValue)
                    .header("User-Agent", USER_AGENT)
                    .build();
            if (log.isDebugEnabled()) {
                log.debug("Performing GET request to \"" + request.url() + "\"");
            }

            Response response = httpClient.newCall(request).execute();

            int responseCode = response.code();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success


                log.debug("Got response, checking token in response");
                String acmeTokenFromHost = response.body().string().split("\\.")[0];

                if (acmeTokenFromHost.equals(expectedAuthTokenValue)) {
                    passed = true;
                    log.info("HTTP Challenge has validated for host \"" + host + "\"");
                } else {
                    log.info("HTTP Challenge validation failed for host \"" + host + "\". Content doesn't match. Expected: " + expectedAuthTokenValue + "; Got: " + acmeTokenFromHost);
                }

            } else {
                log.error("HTTP Challenge failed for host \"" + host + "\", got HTTP status code " + responseCode);

            }
        } catch (IOException e) {
            log.error("HTTP Challenge failed for host \"" + host + "\". Is it reachable?", e);
        }

        return passed;

    }
}
