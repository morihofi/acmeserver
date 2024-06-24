/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.network.scm.github.responses;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents an asset associated with a GitHub release.
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public class Asset {

    /**
     * The creation date of the asset.
     */
    @SerializedName("created_at")
    private String createdAt;

    /**
     * The URL for downloading the asset via a web browser.
     */
    @SerializedName("browser_download_url")
    private String browserDownloadurl;

    /**
     * The API URL for the asset.
     */
    @SerializedName("url")
    private String url;

    /**
     * The number of times the asset has been downloaded.
     */
    @SerializedName("download_count")
    private long downloadCount;

    /**
     * The MIME type of the asset.
     */
    @SerializedName("content_type")
    private String contentType;

    /**
     * The size of the asset in bytes.
     */
    @SerializedName("size")
    private long size;

    /**
     * The date the asset was last updated.
     */
    @SerializedName("updated_at")
    private String updatedAt;

    /**
     * The user who uploaded the asset.
     */
    @SerializedName("uploader")
    private Author uploader;

    /**
     * The name of the asset.
     */
    @SerializedName("name")
    private String name;

    /**
     * The unique identifier for the asset.
     */
    @SerializedName("id")
    private long id;

    /**
     * The state of the asset (e.g., "uploaded").
     */
    @SerializedName("state")
    private String state;

    /**
     * The node ID for the asset.
     */
    @SerializedName("node_id")
    private String nodeid;

    /**
     * Retrieves the creation date of the asset.
     *
     * @return The creation date as a {@code String}.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation date of the asset.
     *
     * @param value The creation date as a {@code String}.
     */
    public void setCreatedAt(String value) {
        this.createdAt = value;
    }

    /**
     * Retrieves the URL for downloading the asset via a web browser.
     *
     * @return The browser download URL as a {@code String}.
     */
    public String getBrowserDownloadurl() {
        return browserDownloadurl;
    }

    /**
     * Sets the URL for downloading the asset via a web browser.
     *
     * @param value The browser download URL as a {@code String}.
     */
    public void setBrowserDownloadurl(String value) {
        this.browserDownloadurl = value;
    }

    /**
     * Retrieves the API URL for the asset.
     *
     * @return The API URL as a {@code String}.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the API URL for the asset.
     *
     * @param value The API URL as a {@code String}.
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Retrieves the number of times the asset has been downloaded.
     *
     * @return The download count as a {@code long}.
     */
    public long getDownloadCount() {
        return downloadCount;
    }

    /**
     * Sets the number of times the asset has been downloaded.
     *
     * @param value The download count as a {@code long}.
     */
    public void setDownloadCount(long value) {
        this.downloadCount = value;
    }

    /**
     * Retrieves the MIME type of the asset.
     *
     * @return The MIME type as a {@code String}.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the MIME type of the asset.
     *
     * @param value The MIME type as a {@code String}.
     */
    public void setContentType(String value) {
        this.contentType = value;
    }

    /**
     * Retrieves the size of the asset in bytes.
     *
     * @return The size as a {@code long}.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size of the asset in bytes.
     *
     * @param value The size as a {@code long}.
     */
    public void setSize(long value) {
        this.size = value;
    }

    /**
     * Retrieves the date the asset was last updated.
     *
     * @return The last updated date as a {@code String}.
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the date the asset was last updated.
     *
     * @param value The last updated date as a {@code String}.
     */
    public void setUpdatedAt(String value) {
        this.updatedAt = value;
    }

    /**
     * Retrieves the user who uploaded the asset.
     *
     * @return The uploader as an {@link Author}.
     */
    public Author getUploader() {
        return uploader;
    }

    /**
     * Sets the user who uploaded the asset.
     *
     * @param value The uploader as an {@link Author}.
     */
    public void setUploader(Author value) {
        this.uploader = value;
    }

    /**
     * Retrieves the name of the asset.
     *
     * @return The name as a {@code String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the asset.
     *
     * @param value The name as a {@code String}.
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Retrieves the unique identifier for the asset.
     *
     * @return The unique identifier as a {@code long}.
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the asset.
     *
     * @param value The unique identifier as a {@code long}.
     */
    public void setId(long value) {
        this.id = value;
    }

    /**
     * Retrieves the state of the asset.
     *
     * @return The state as a {@code String}.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state of the asset.
     *
     * @param value The state as a {@code String}.
     */
    public void setState(String value) {
        this.state = value;
    }

    /**
     * Retrieves the node ID for the asset.
     *
     * @return The node ID as a {@code String}.
     */
    public String getNodeid() {
        return nodeid;
    }

    /**
     * Sets the node ID for the asset.
     *
     * @param value The node ID as a {@code String}.
     */
    public void setNodeid(String value) {
        this.nodeid = value;
    }
}
