package de.morihofi.acmeserver.certificate.acmeapi;

import de.morihofi.acmeserver.Main;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;

public class HTTPChallenge {

    public static final Logger log = LogManager.getLogger(HTTPChallenge.class);
    private static final OkHttpClient httpClient = new OkHttpClient();

    private static final String USER_AGENT = "Mozilla/5.0 ACMEServer/" + Main.buildMetadataVersion + " Java/" + System.getProperty("java.version");
    public static boolean check(String expectedAuthTokenId, String expectedAuthTokenValue, String host){
        boolean passed = false;

        try {

            // avoid creating several instances, should be singleon


            Request request = new Request.Builder()
                    .url("http://" + host + "/.well-known/acme-challenge/" + expectedAuthTokenId)
                    .header("User-Agent", USER_AGENT)
                    .build();
            if(log.isDebugEnabled()){
                log.debug("Performing GET request to \"" + request.url() + "\"");
            }

            Response response = httpClient.newCall(request).execute();

            int responseCode = response.code();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success


                log.debug("Got response, checking token in response");
                String acmeTokenFromHost = response.body().string().split("\\.")[0];

                if (acmeTokenFromHost.equals(expectedAuthTokenValue)){
                    passed = true;
                    log.info("HTTP Challenge has validated for host \"" + host + "\"");
                }else {
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
