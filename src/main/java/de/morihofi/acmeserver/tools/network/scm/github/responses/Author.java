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

public class Author {
    @SerializedName("gists_url")
    private String gistsurl;
    @SerializedName("repos_url")
    private String reposurl;
    @SerializedName("following_url")
    private String followingurl;
    @SerializedName("starred_url")
    private String starredurl;
    @SerializedName("login")
    private String login;
    @SerializedName("followers_url")
    private String followersurl;
    @SerializedName("type")
    private String type;
    @SerializedName("url")
    private String url;
    @SerializedName("subscriptions_url")
    private String subscriptionsurl;
    @SerializedName("received_events_url")
    private String receivedEventsurl;
    @SerializedName("avatar_url")
    private String avatarurl;
    @SerializedName("events_url")
    private String eventsurl;
    @SerializedName("html_url")
    private String htmlurl;
    @SerializedName("site_admin")
    private boolean siteAdmin;
    @SerializedName("id")
    private long id;
    @SerializedName("gravatar_id")
    private String gravatarid;
    @SerializedName("node_id")
    private String nodeid;
    @SerializedName("organizations_url")
    private String organizationsurl;

    public String getGistsurl() {return gistsurl;}

    public void setGistsurl(String value) {this.gistsurl = value;}

    public String getReposurl() {return reposurl;}

    public void setReposurl(String value) {this.reposurl = value;}

    public String getFollowingurl() {return followingurl;}

    public void setFollowingurl(String value) {this.followingurl = value;}

    public String getStarredurl() {return starredurl;}

    public void setStarredurl(String value) {this.starredurl = value;}

    public String getLogin() {return login;}

    public void setLogin(String value) {this.login = value;}

    public String getFollowersurl() {return followersurl;}

    public void setFollowersurl(String value) {this.followersurl = value;}

    public String getType() {return type;}

    public void setType(String value) {this.type = value;}

    public String geturl() {return url;}

    public void seturl(String value) {this.url = value;}

    public String getSubscriptionsurl() {return subscriptionsurl;}

    public void setSubscriptionsurl(String value) {this.subscriptionsurl = value;}

    public String getReceivedEventsurl() {return receivedEventsurl;}

    public void setReceivedEventsurl(String value) {this.receivedEventsurl = value;}

    public String getAvatarurl() {return avatarurl;}

    public void setAvatarurl(String value) {this.avatarurl = value;}

    public String getEventsurl() {return eventsurl;}

    public void setEventsurl(String value) {this.eventsurl = value;}

    public String getHtmlurl() {return htmlurl;}

    public void setHtmlurl(String value) {this.htmlurl = value;}

    public boolean getSiteAdmin() {return siteAdmin;}

    public void setSiteAdmin(boolean value) {this.siteAdmin = value;}

    public long getid() {return id;}

    public void setid(long value) {this.id = value;}

    public String getGravatarid() {return gravatarid;}

    public void setGravatarid(String value) {this.gravatarid = value;}

    public String getNodeid() {return nodeid;}

    public void setNodeid(String value) {this.nodeid = value;}

    public String getOrganizationsurl() {return organizationsurl;}

    public void setOrganizationsurl(String value) {this.organizationsurl = value;}
}
