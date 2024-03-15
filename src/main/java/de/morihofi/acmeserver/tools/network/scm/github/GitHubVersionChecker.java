package de.morihofi.acmeserver.tools.network.scm.github;

import com.google.gson.Gson;
import de.morihofi.acmeserver.tools.network.scm.github.responses.Release;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class GitHubVersionChecker {
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final String PROJECT_NAME = "acmeserver";
    private static final String PROJECT_OWNER = "morihofi";

    public static String getLatestReleaseTag() throws IOException {
        return getLatestReleaseTag(PROJECT_OWNER, PROJECT_NAME);
    }

    public static String getLatestReleaseURL() throws IOException {
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
            return new Gson().fromJson(response.body().string(), Release.class).getTagName();
        }
    }
}
