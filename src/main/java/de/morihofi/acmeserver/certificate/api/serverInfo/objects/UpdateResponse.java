package de.morihofi.acmeserver.certificate.api.serverInfo.objects;

public class UpdateResponse {
    private boolean updateAvailable = false;
    private String releasesUrl;

    public UpdateResponse(boolean updateAvailable, String releasesUrl) {
        this.updateAvailable = updateAvailable;
        this.releasesUrl = releasesUrl;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public String getReleasesUrl() {
        return releasesUrl;
    }

    public void setReleasesUrl(String releasesUrl) {
        this.releasesUrl = releasesUrl;
    }
}
