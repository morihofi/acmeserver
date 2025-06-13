package de.morihofi.acmeserver.ct;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Simple client for submitting certificates to a Certificate Transparency log.
 */
@Slf4j
@RequiredArgsConstructor
public class CertificateTransparencyClient {
    private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient httpClient;
    private final String logServer;

    /**
     * Submit a certificate chain to the CT log.
     *
     * @param chain           certificate chain starting with the leaf certificate
     * @param preCertificate  whether to submit using add-pre-chain
     * @throws IOException if submission fails
     */
    public void submitChain(@NonNull List<X509Certificate> chain, boolean preCertificate) throws IOException {
        String url = logServer + (preCertificate ? "/ct/v1/add-pre-chain" : "/ct/v1/add-chain");
        JsonArray arr = new JsonArray();
        for (X509Certificate cert : chain) {
            try {
                arr.add(Base64.getEncoder().encodeToString(cert.getEncoded()));
            } catch (CertificateEncodingException e) {
                throw new IOException("Failed to encode certificate", e);
            }
        }
        JsonObject obj = new JsonObject();
        obj.add("chain", arr);
        String json = new Gson().toJson(obj);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("CT log responded with status " + response.code());
            }
        }
    }
}
