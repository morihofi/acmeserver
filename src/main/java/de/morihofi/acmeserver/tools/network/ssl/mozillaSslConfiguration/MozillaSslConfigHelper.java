/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version4dot0up.MozillaSslConfiguration4dot4upResponse;
import de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version5dot1up.MozillaSslConfiguration5dot1upResponse;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MozillaSslConfigHelper {
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    private static final OkHttpClient okHttpClient = new OkHttpClient();
    private static final Gson mozillaSslGson = new Gson();

    public static BasicConfiguration getLatestConfigurationGuidelines(CONFIGURATION configuration) throws IOException {
        return getConfigurationGuidelinesForVersion("latest", configuration);
    }

    public static BasicConfiguration getConfigurationGuidelinesForVersion(String version, CONFIGURATION configuration) throws IOException {

        String guideLineJson = null;

        // Look in resources, if not found then ask online
        {

            InputStream is = null;
            switch (version) {
                case "4.0" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/4.0.json");
                case "5.0" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.0.json");

                case "5.1" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.1.json");
                case "5.2" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.2.json");
                case "5.3" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.3.json");
                case "5.4" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.4.json");
                case "5.5" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.5.json");
                case "5.6" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.6.json");
                case "5.7" -> is = MozillaSslConfigHelper.class.getResourceAsStream("/mozillaSslConfig/5.7.json");
            }

            if (is != null) {
                StringBuilder textBuilder = new StringBuilder();
                try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    int c = 0;
                    while ((c = reader.read()) != -1) {
                        textBuilder.append((char) c);
                    }
                }
                guideLineJson = textBuilder.toString();
            }
        }

        if (guideLineJson == null) {
            Request request = new Request.Builder()
                    .url("https://ssl-config.mozilla.org/guidelines/" + version + ".json")
                    .build();

            Call call = okHttpClient.newCall(request);

            try (
                    Response response = call.execute()) {

                if (!response.isSuccessful()) {
                    throw new IOException("Response was no successful: Code " + response.code());
                }

                assert response.body() != null;
                guideLineJson = response.body().string();
            }
        }

        // Analyze the format
        JsonObject jsonObject = JsonParser.parseString(guideLineJson).getAsJsonObject();
        String versionFromJson = jsonObject.get("version").getAsString();
        if (versionFromJson.equals("4.0") || versionFromJson.equals("5.0")) {
            // Old format

            MozillaSslConfiguration4dot4upResponse response =
                    mozillaSslGson.fromJson(guideLineJson, MozillaSslConfiguration4dot4upResponse.class);

            de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version4dot0up.Configuration mozConfig =
                    switch (configuration) {
                        case MODERN -> response.getConfigurations().getModern();
                        case INTERMEDIATE -> response.getConfigurations().getIntermediate();
                        case OLD -> response.getConfigurations().getOld();
                    };

            Set<String> ciphers = new HashSet<>(mozConfig.getCiphersuites());
            Set<String> protocols = new HashSet<>(mozConfig.getTlsVersions());

            return new BasicConfiguration(response.getVersion(), response.getHref(), ciphers, protocols, mozConfig.getHstsMinAge(),
                    mozConfig.getOldestClients());
        } else {
            // New format

            MozillaSslConfiguration5dot1upResponse response =
                    mozillaSslGson.fromJson(guideLineJson, MozillaSslConfiguration5dot1upResponse.class);
            de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version5dot1up.Configuration mozConfig =
                    switch (configuration) {
                        case MODERN -> response.getConfigurations().getModern();
                        case INTERMEDIATE -> response.getConfigurations().getIntermediate();
                        case OLD -> response.getConfigurations().getOld();
                    };

            // Set Cipher Suites and Protokolls based on Mozilla's recommendations
            // We need to concatenate this into a single list, otherwise TLS 1.3 won't work

            Set<String> ciphers = new HashSet<>();
            ciphers.addAll(mozConfig.getCiphers().getIana());
            ciphers.addAll(mozConfig.getCiphersuites());

            Set<String> protocols = new HashSet<>(mozConfig.getTlsVersions());

            return new BasicConfiguration(response.getVersion(), response.getHref(), ciphers, protocols, mozConfig.getHstsMinAge(),
                    mozConfig.getOldestClients());
        }
    }

    public enum CONFIGURATION {
        MODERN, INTERMEDIATE, OLD
    }

    public record BasicConfiguration(double version, String href, Set<String> ciphers, Set<String> protocols, long hstsMinAge,
            List<String> oldestClients) {
    }
}
