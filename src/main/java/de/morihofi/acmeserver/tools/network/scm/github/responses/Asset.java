package de.morihofi.acmeserver.tools.network.scm.github.responses;


import com.google.gson.annotations.SerializedName;

public class Asset {
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("browser_download_url")
    private String browserDownloadurl;
    @SerializedName("url")
    private String url;
    @SerializedName("download_count")
    private long downloadCount;
    @SerializedName("content_type")
    private String contentType;
    @SerializedName("size")
    private long size;
    @SerializedName("updated_at")
    private String updatedAt;
    @SerializedName("uploader")
    private Author uploader;
    @SerializedName("name")
    private String name;
    @SerializedName("id")
    private long id;
    @SerializedName("state")
    private String state;
    @SerializedName("node_id")
    private String nodeid;

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String value) { this.createdAt = value; }

    public String getBrowserDownloadurl() { return browserDownloadurl; }
    public void setBrowserDownloadurl(String value) { this.browserDownloadurl = value; }

    public String geturl() { return url; }
    public void seturl(String value) { this.url = value; }

    public long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(long value) { this.downloadCount = value; }

    public String getContentType() { return contentType; }
    public void setContentType(String value) { this.contentType = value; }

    public long getSize() { return size; }
    public void setSize(long value) { this.size = value; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String value) { this.updatedAt = value; }

    public Author getUploader() { return uploader; }
    public void setUploader(Author value) { this.uploader = value; }

    public String getName() { return name; }
    public void setName(String value) { this.name = value; }

    public long getid() { return id; }
    public void setid(long value) { this.id = value; }

    public String getState() { return state; }
    public void setState(String value) { this.state = value; }

    public String getNodeid() { return nodeid; }
    public void setNodeid(String value) { this.nodeid = value; }
}
