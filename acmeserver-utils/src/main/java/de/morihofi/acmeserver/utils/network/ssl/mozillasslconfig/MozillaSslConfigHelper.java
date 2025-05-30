package de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.response.version4dot0up.Configuration;
import de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.response.version4dot0up.MozillaSslConfiguration4dot4upResponse;
import de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.response.version5dot1up.MozillaSslConfiguration5dot1upResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class for retrieving and parsing Mozilla SSL configuration guidelines.
 * <p>
 * This refactored version removes the brittle version switch‑statement, adds default
 * handling to all switch expressions and extracts common logic into helper methods
 * which makes the code shorter, easier to maintain and fully covered by the compiler.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MozillaSslConfigHelper {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient(); // TODO: migrate to network client class
    private static final Gson GSON = new Gson();

    private static final String RESOURCE_PREFIX = "/mozillaSslConfig/";
    private static final String RESOURCE_SUFFIX = ".json";

    /**
     * Retrieves the latest configuration guidelines for a given configuration type.
     *
     * @param configuration the type of configuration (MODERN, INTERMEDIATE, OLD).
     * @return a BasicConfiguration object containing the guidelines.
     * @throws IOException if an I/O error occurs.
     */
    public static BasicConfiguration getLatestConfigurationGuidelines(@NonNull CONFIGURATION configuration) throws IOException {
        return getConfigurationGuidelinesForVersion("latest", configuration);
    }

    /**
     * Retrieves the configuration guidelines for a specific version and configuration type.
     *
     * @param version       the version of the guidelines to retrieve; use "latest" for the newest one.
     * @param configuration the type of configuration (MODERN, INTERMEDIATE, OLD).
     * @return a BasicConfiguration object containing the guidelines.
     * @throws IOException if an I/O error occurs.
     */
    public static BasicConfiguration getConfigurationGuidelinesForVersion(@NonNull String version, @NonNull CONFIGURATION configuration) throws IOException {
        // 1) try bundled resources
        String guidelineJson = readGuidelineFromClasspath(version);

        // 2) fall back to the remote endpoint
        if (guidelineJson == null) {
            guidelineJson = readGuidelineFromWeb(version);
        }

        // 3) parse and map the JSON to our DTO
        return parseGuidelineJson(guidelineJson, configuration);
    }

    /* ------------------------------------------------ helper methods --------------------------------------------- */
    private static String readGuidelineFromClasspath(@NonNull String version) {
        String resource = RESOURCE_PREFIX + version + RESOURCE_SUFFIX;
        try (InputStream in = MozillaSslConfigHelper.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null; // not shipped with the JAR
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Failed to read resource {} – continuing with network fallback", resource, e);
            return null;
        }
    }

    /**
     * Reads the guideline JSON from the Mozilla SSL configuration web service.
     *
     * @param version the version of the guidelines to retrieve.
     * @return the JSON string containing the guidelines.
     * @throws IOException if an I/O error occurs while fetching the data.
     */
    @NonNull
    private static String readGuidelineFromWeb(@NonNull String version) throws IOException {
        Request request = new Request.Builder().url("https://ssl-config.mozilla.org/guidelines/" + version + ".json").build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request to ssl-config.mozilla.org failed with HTTP " + response.code());
            }
            return Objects.requireNonNull(response.body()).string();
        }
    }

    /**
     * Parses the JSON string into a BasicConfiguration object.
     *
     * @param json          the JSON string to parse.
     * @param configuration the type of configuration (MODERN, INTERMEDIATE, OLD).
     * @return a BasicConfiguration object containing the parsed data.
     */
    @NonNull
    private static BasicConfiguration parseGuidelineJson(@NonNull String json, @NonNull CONFIGURATION configuration) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        String versionInJson = jsonObject.get("version").getAsString();

        if ("4.0".equals(versionInJson) || "5.0".equals(versionInJson)) {
            // Older 4.x/5.0 schema
            MozillaSslConfiguration4dot4upResponse resp = GSON.fromJson(json, MozillaSslConfiguration4dot4upResponse.class);

            Configuration mozCfg = mapConfiguration(configuration, resp);
            return new BasicConfiguration(resp.getVersion(), resp.getHref(), new HashSet<>(mozCfg.getCiphersuites()), new HashSet<>(mozCfg.getTlsVersions()), mozCfg.getHstsMinAge(), mozCfg.getOldestClients());
        }

        // Newer (≥ 5.1) schema
        MozillaSslConfiguration5dot1upResponse resp = GSON.fromJson(json, MozillaSslConfiguration5dot1upResponse.class);

        de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.response.version5dot1up.Configuration mozCfg = mapConfiguration(configuration, resp);

        Set<String> ciphers = new HashSet<>(mozCfg.getCiphersuites());
        // Concatenate IANA names needed for TLS 1.3
        if (mozCfg.getCiphers() != null) {
            ciphers.addAll(mozCfg.getCiphers().getIana());
        }

        return new BasicConfiguration(resp.getVersion(), resp.getHref(), ciphers, new HashSet<>(mozCfg.getTlsVersions()), mozCfg.getHstsMinAge(), mozCfg.getOldestClients());
    }

    @NonNull
    private static Configuration mapConfiguration(@NonNull CONFIGURATION type, @NonNull MozillaSslConfiguration4dot4upResponse resp) {
        return switch (type) {
            case MODERN -> resp.getConfigurations().getModern();
            case INTERMEDIATE -> resp.getConfigurations().getIntermediate();
            case OLD -> resp.getConfigurations().getOld();
            default -> throw new IllegalStateException("Unhandled configuration type: " + type);
        };
    }

    @NonNull
    private static de.morihofi.acmeserver.utils.network.ssl.mozillasslconfig.response.version5dot1up.Configuration mapConfiguration(@NonNull CONFIGURATION type, @NonNull MozillaSslConfiguration5dot1upResponse resp) {
        return switch (type) {
            case MODERN -> resp.getConfigurations().getModern();
            case INTERMEDIATE -> resp.getConfigurations().getIntermediate();
            case OLD -> resp.getConfigurations().getOld();
            default -> throw new IllegalStateException("Unhandled configuration type: " + type);
        };
    }

    /* -------------------------------------------------- DTOs ------------------------------------------------------ */

    public enum CONFIGURATION {
        MODERN, INTERMEDIATE, OLD
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record BasicConfiguration(double version, String href, Set<String> ciphers, Set<String> protocols,
                                     long hstsMinAge, List<String> oldestClients) {
    }
}
