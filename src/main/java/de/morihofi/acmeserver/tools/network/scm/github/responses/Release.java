package de.morihofi.acmeserver.tools.network.scm.github.responses;

import java.util.List;


public class Release {
    private Author author;
    private String tagName;
    private String createdAt;
    private long mentionsCount;
    private String body;
    private String url;
    private String assetsurl;
    private List<Asset> assets;
    private boolean prerelease;
    private String htmlurl;
    private String targetCommitish;
    private boolean draft;
    private String zipballurl;
    private String name;
    private String uploadurl;
    private long id;
    private String publishedAt;
    private String tarballurl;
    private String nodeid;

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author value) {
        this.author = value;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String value) {
        this.tagName = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String value) {
        this.createdAt = value;
    }

    public long getMentionsCount() {
        return mentionsCount;
    }

    public void setMentionsCount(long value) {
        this.mentionsCount = value;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String value) {
        this.body = value;
    }

    public String geturl() {
        return url;
    }

    public void seturl(String value) {
        this.url = value;
    }

    public String getAssetsurl() {
        return assetsurl;
    }

    public void setAssetsurl(String value) {
        this.assetsurl = value;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> value) {
        this.assets = value;
    }

    public boolean getPrerelease() {
        return prerelease;
    }

    public void setPrerelease(boolean value) {
        this.prerelease = value;
    }

    public String getHtmlurl() {
        return htmlurl;
    }

    public void setHtmlurl(String value) {
        this.htmlurl = value;
    }

    public String getTargetCommitish() {
        return targetCommitish;
    }

    public void setTargetCommitish(String value) {
        this.targetCommitish = value;
    }

    public boolean getDraft() {
        return draft;
    }

    public void setDraft(boolean value) {
        this.draft = value;
    }

    public String getZipballurl() {
        return zipballurl;
    }

    public void setZipballurl(String value) {
        this.zipballurl = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getUploadurl() {
        return uploadurl;
    }

    public void setUploadurl(String value) {
        this.uploadurl = value;
    }

    public long getid() {
        return id;
    }

    public void setid(long value) {
        this.id = value;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String value) {
        this.publishedAt = value;
    }

    public String getTarballurl() {
        return tarballurl;
    }

    public void setTarballurl(String value) {
        this.tarballurl = value;
    }

    public String getNodeid() {
        return nodeid;
    }

    public void setNodeid(String value) {
        this.nodeid = value;
    }
}
