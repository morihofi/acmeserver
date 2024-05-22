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

package de.morihofi.acmeserver.tools.network.scm.github.responses;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("EI_EXPOSE_REP")
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

    public String getCreatedAt() {return createdAt;}

    public void setCreatedAt(String value) {this.createdAt = value;}

    public String getBrowserDownloadurl() {return browserDownloadurl;}

    public void setBrowserDownloadurl(String value) {this.browserDownloadurl = value;}

    public String geturl() {return url;}

    public void seturl(String value) {this.url = value;}

    public long getDownloadCount() {return downloadCount;}

    public void setDownloadCount(long value) {this.downloadCount = value;}

    public String getContentType() {return contentType;}

    public void setContentType(String value) {this.contentType = value;}

    public long getSize() {return size;}

    public void setSize(long value) {this.size = value;}

    public String getUpdatedAt() {return updatedAt;}

    public void setUpdatedAt(String value) {this.updatedAt = value;}

    public Author getUploader() {return uploader;}

    public void setUploader(Author value) {this.uploader = value;}

    public String getName() {return name;}

    public void setName(String value) {this.name = value;}

    public long getid() {return id;}

    public void setid(long value) {this.id = value;}

    public String getState() {return state;}

    public void setState(String value) {this.state = value;}

    public String getNodeid() {return nodeid;}

    public void setNodeid(String value) {this.nodeid = value;}
}
