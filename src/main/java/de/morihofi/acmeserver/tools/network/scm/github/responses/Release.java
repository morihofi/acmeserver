package de.morihofi.acmeserver.tools.network.scm.github.responses;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Release {
    @SerializedName("author")
    private Author author;

    @SerializedName("tag_name")
    private String tagName;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("mentions_count")
    private long mentionsCount;

    @SerializedName("body")
    private String body;
    @SerializedName("url")
    private String url;

    @SerializedName("assets_url")
    private String assetsurl;

    @SerializedName("assets")
    private List<Asset> assets;

    @SerializedName("prerelease")
    private boolean prerelease;

    @SerializedName("html_url")
    private String htmlurl;

    @SerializedName("target_commitish")
    private String targetCommitish;

    @SerializedName("draft")
    private boolean draft;

    @SerializedName("zipball_url")
    private String zipballurl;

    @SerializedName("name")
    private String name;

    @SerializedName("upload_url")
    private String uploadurl;
    @SerializedName("id")
    private long id;
    @SerializedName("published_at")
    private String publishedAt;

    @SerializedName("tarball_url")
    private String tarballurl;
    @SerializedName("node_id")
    private String nodeid;

    public Author getAuthor() {return author;}

    public void setAuthor(Author value) {this.author = value;}

    public String getTagName() {return tagName;}

    public void setTagName(String value) {this.tagName = value;}

    public String getCreatedAt() {return createdAt;}

    public void setCreatedAt(String value) {this.createdAt = value;}

    public long getMentionsCount() {return mentionsCount;}

    public void setMentionsCount(long value) {this.mentionsCount = value;}

    public String getBody() {return body;}

    public void setBody(String value) {this.body = value;}

    public String geturl() {return url;}

    public void seturl(String value) {this.url = value;}

    public String getAssetsurl() {return assetsurl;}

    public void setAssetsurl(String value) {this.assetsurl = value;}

    public List<Asset> getAssets() {return assets;}

    public void setAssets(List<Asset> value) {this.assets = value;}

    public boolean getPrerelease() {return prerelease;}

    public void setPrerelease(boolean value) {this.prerelease = value;}

    public String getHtmlurl() {return htmlurl;}

    public void setHtmlurl(String value) {this.htmlurl = value;}

    public String getTargetCommitish() {return targetCommitish;}

    public void setTargetCommitish(String value) {this.targetCommitish = value;}

    public boolean getDraft() {return draft;}

    public void setDraft(boolean value) {this.draft = value;}

    public String getZipballurl() {return zipballurl;}

    public void setZipballurl(String value) {this.zipballurl = value;}

    public String getName() {return name;}

    public void setName(String value) {this.name = value;}

    public String getUploadurl() {return uploadurl;}

    public void setUploadurl(String value) {this.uploadurl = value;}

    public long getid() {return id;}

    public void setid(long value) {this.id = value;}

    public String getPublishedAt() {return publishedAt;}

    public void setPublishedAt(String value) {this.publishedAt = value;}

    public String getTarballurl() {return tarballurl;}

    public void setTarballurl(String value) {this.tarballurl = value;}

    public String getNodeid() {return nodeid;}

    public void setNodeid(String value) {this.nodeid = value;}
}
