package de.morihofi.acmeserver.tools.network.scm.github;

import com.google.gson.Gson;
import de.morihofi.acmeserver.tools.network.scm.github.responses.Release;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class GitHubVersionChecker {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final String PROJECT_NAME = "acmeserver";
    private static final String PROJECT_OWNER = "morihofi";

    public static String getLatestReleaseTag() throws IOException {
        return getLatestReleaseTag(PROJECT_OWNER, PROJECT_NAME);
    }

    public static String getLatestReleaseURL() {
        return "https://github.com/" + PROJECT_OWNER + "/" + PROJECT_NAME + "/releases/latest";
    }

    public static String getLatestReleaseTag(String owner, String repo) throws IOException {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            assert response.body() != null;
            String responseString = response.body().string();

            return new Gson().fromJson(responseString, Release.class).getTagName();
        }
    }
}
