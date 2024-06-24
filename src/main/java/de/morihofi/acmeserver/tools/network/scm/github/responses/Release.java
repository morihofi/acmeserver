/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.network.scm.github.responses;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents a release in a GitHub repository. This class captures the details of a release,
 * including author information, tag name, creation date, release notes, and assets associated
 * with the release.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Release {

    /**
     * The author of the release.
     */
    @SerializedName("author")
    private Author author;

    /**
     * The tag name of the release.
     */
    @SerializedName("tag_name")
    private String tagName;

    /**
     * The creation date of the release.
     */
    @SerializedName("created_at")
    private String createdAt;

    /**
     * The count of mentions in the release notes.
     */
    @SerializedName("mentions_count")
    private long mentionsCount;

    /**
     * The body text of the release notes.
     */
    @SerializedName("body")
    private String body;

    /**
     * The URL of the release.
     */
    @SerializedName("url")
    private String url;

    /**
     * The URL for the assets of the release.
     */
    @SerializedName("assets_url")
    private String assetsurl;

    /**
     * The list of assets associated with the release.
     */
    @SerializedName("assets")
    private List<Asset> assets;

    /**
     * Indicates whether the release is a pre-release.
     */
    @SerializedName("prerelease")
    private boolean prerelease;

    /**
     * The HTML URL of the release.
     */
    @SerializedName("html_url")
    private String htmlurl;

    /**
     * The target commitish value of the release.
     */
    @SerializedName("target_commitish")
    private String targetCommitish;

    /**
     * Indicates whether the release is a draft.
     */
    @SerializedName("draft")
    private boolean draft;

    /**
     * The URL to download the release as a zipball.
     */
    @SerializedName("zipball_url")
    private String zipballurl;

    /**
     * The name of the release.
     */
    @SerializedName("name")
    private String name;

    /**
     * The URL to upload assets to the release.
     */
    @SerializedName("upload_url")
    private String uploadurl;

    /**
     * The ID of the release.
     */
    @SerializedName("id")
    private long id;

    /**
     * The publication date of the release.
     */
    @SerializedName("published_at")
    private String publishedAt;

    /**
     * The URL to download the release as a tarball.
     */
    @SerializedName("tarball_url")
    private String tarballurl;

    /**
     * The node ID of the release.
     */
    @SerializedName("node_id")
    private String nodeid;

    /**
     * Gets the author of the release.
     *
     * @return The author of the release.
     */
    public Author getAuthor() {
        return author;
    }

    /**
     * Sets the author of the release.
     *
     * @param value The author to set.
     */
    public void setAuthor(Author value) {
        this.author = value;
    }

    /**
     * Gets the tag name of the release.
     *
     * @return The tag name of the release.
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Sets the tag name of the release.
     *
     * @param value The tag name to set.
     */
    public void setTagName(String value) {
        this.tagName = value;
    }

    /**
     * Gets the creation date of the release.
     *
     * @return The creation date of the release.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation date of the release.
     *
     * @param value The creation date to set.
     */
    public void setCreatedAt(String value) {
        this.createdAt = value;
    }

    /**
     * Gets the mentions count of the release.
     *
     * @return The mentions count of the release.
     */
    public long getMentionsCount() {
        return mentionsCount;
    }

    /**
     * Sets the mentions count of the release.
     *
     * @param value The mentions count to set.
     */
    public void setMentionsCount(long value) {
        this.mentionsCount = value;
    }

    /**
     * Gets the body text of the release notes.
     *
     * @return The body text of the release notes.
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body text of the release notes.
     *
     * @param value The body text to set.
     */
    public void setBody(String value) {
        this.body = value;
    }

    /**
     * Gets the URL of the release.
     *
     * @return The URL of the release.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL of the release.
     *
     * @param value The URL to set.
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the URL for the assets of the release.
     *
     * @return The URL for the assets of the release.
     */
    public String getAssetsurl() {
        return assetsurl;
    }

    /**
     * Sets the URL for the assets of the release.
     *
     * @param value The URL for the assets to set.
     */
    public void setAssetsurl(String value) {
        this.assetsurl = value;
    }

    /**
     * Gets the list of assets associated with the release.
     *
     * @return The list of assets.
     */
    public List<Asset> getAssets() {
        return assets;
    }

    /**
     * Sets the list of assets associated with the release.
     *
     * @param value The list of assets to set.
     */
    public void setAssets(List<Asset> value) {
        this.assets = value;
    }

    /**
     * Checks if the release is a pre-release.
     *
     * @return true if it is a pre-release, false otherwise.
     */
    public boolean getPrerelease() {
        return prerelease;
    }

    /**
     * Sets whether the release is a pre-release.
     *
     * @param value The pre-release status to set.
     */
    public void setPrerelease(boolean value) {
        this.prerelease = value;
    }

    /**
     * Gets the HTML URL of the release.
     *
     * @return The HTML URL of the release.
     */
    public String getHtmlurl() {
        return htmlurl;
    }

    /**
     * Sets the HTML URL of the release.
     *
     * @param value The HTML URL to set.
     */
    public void setHtmlurl(String value) {
        this.htmlurl = value;
    }

    /**
     * Gets the target commitish value of the release.
     *
     * @return The target commitish value.
     */
    public String getTargetCommitish() {
        return targetCommitish;
    }

    /**
     * Sets the target commitish value of the release.
     *
     * @param value The target commitish value to set.
     */
    public void setTargetCommitish(String value) {
        this.targetCommitish = value;
    }

    /**
     * Checks if the release is a draft.
     *
     * @return true if it is a draft, false otherwise.
     */
    public boolean getDraft() {
        return draft;
    }

    /**
     * Sets whether the release is a draft.
     *
     * @param value The draft status to set.
     */
    public void setDraft(boolean value) {
        this.draft = value;
    }

    /**
     * Gets the URL to download the release as a zipball.
     *
     * @return The URL to download the release as a zipball.
     */
    public String getZipballurl() {
        return zipballurl;
    }

    /**
     * Sets the URL to download the release as a zipball.
     *
     * @param value The URL to set.
     */
    public void setZipballurl(String value) {
        this.zipballurl = value;
    }

    /**
     * Gets the name of the release.
     *
     * @return The name of the release.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the release.
     *
     * @param value The name to set.
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the URL to upload assets to the release.
     *
     * @return The URL to upload assets.
     */
    public String getUploadurl() {
        return uploadurl;
    }

    /**
     * Sets the URL to upload assets to the release.
     *
     * @param value The URL to set.
     */
    public void setUploadurl(String value) {
        this.uploadurl = value;
    }

    /**
     * Gets the ID of the release.
     *
     * @return The ID of the release.
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the ID of the release.
     *
     * @param value The ID to set.
     */
    public void setId(long value) {
        this.id = value;
    }

    /**
     * Gets the publication date of the release.
     *
     * @return The publication date of the release.
     */
    public String getPublishedAt() {
        return publishedAt;
    }

    /**
     * Sets the publication date of the release.
     *
     * @param value The publication date to set.
     */
    public void setPublishedAt(String value) {
        this.publishedAt = value;
    }

    /**
     * Gets the URL to download the release as a tarball.
     *
     * @return The URL to download the release as a tarball.
     */
    public String getTarballurl() {
        return tarballurl;
    }

    /**
     * Sets the URL to download the release as a tarball.
     *
     * @param value The URL to set.
     */
    public void setTarballurl(String value) {
        this.tarballurl = value;
    }

    /**
     * Gets the node ID of the release.
     *
     * @return The node ID of the release.
     */
    public String getNodeid() {
        return nodeid;
    }

    /**
     * Sets the node ID of the release.
     *
     * @param value The node ID to set.
     */
    public void setNodeid(String value) {
        this.nodeid = value;
    }
}
