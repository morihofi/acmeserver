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

/**
 * Represents an author in the context of a GitHub response.
 */
public class Author {
    /**
     * The URL for the author's gists.
     */
    @SerializedName("gists_url")
    private String gistsurl;

    /**
     * The URL for the author's repositories.
     */
    @SerializedName("repos_url")
    private String reposurl;

    /**
     * The URL for the author's following list.
     */
    @SerializedName("following_url")
    private String followingurl;

    /**
     * The URL for the author's starred repositories.
     */
    @SerializedName("starred_url")
    private String starredurl;

    /**
     * The login name of the author.
     */
    @SerializedName("login")
    private String login;

    /**
     * The URL for the author's followers list.
     */
    @SerializedName("followers_url")
    private String followersurl;

    /**
     * The type of the author (e.g., User, Organization).
     */
    @SerializedName("type")
    private String type;

    /**
     * The URL for the author.
     */
    @SerializedName("url")
    private String url;

    /**
     * The URL for the author's subscriptions.
     */
    @SerializedName("subscriptions_url")
    private String subscriptionsurl;

    /**
     * The URL for the author's received events.
     */
    @SerializedName("received_events_url")
    private String receivedEventsurl;

    /**
     * The URL for the author's avatar image.
     */
    @SerializedName("avatar_url")
    private String avatarurl;

    /**
     * The URL for the author's events.
     */
    @SerializedName("events_url")
    private String eventsurl;

    /**
     * The URL for the author's GitHub profile.
     */
    @SerializedName("html_url")
    private String htmlurl;

    /**
     * Whether the author is a site administrator.
     */
    @SerializedName("site_admin")
    private boolean siteAdmin;

    /**
     * The unique identifier for the author.
     */
    @SerializedName("id")
    private long id;

    /**
     * The gravatar ID of the author.
     */
    @SerializedName("gravatar_id")
    private String gravatarid;

    /**
     * The node ID for the author.
     */
    @SerializedName("node_id")
    private String nodeid;

    /**
     * The URL for the author's organizations.
     */
    @SerializedName("organizations_url")
    private String organizationsurl;

    /**
     * Retrieves the URL for the author's gists.
     *
     * @return The gists URL as a {@code String}.
     */
    public String getGistsurl() {
        return gistsurl;
    }

    /**
     * Sets the URL for the author's gists.
     *
     * @param value The gists URL as a {@code String}.
     */
    public void setGistsurl(String value) {
        this.gistsurl = value;
    }

    /**
     * Retrieves the URL for the author's repositories.
     *
     * @return The repos URL as a {@code String}.
     */
    public String getReposurl() {
        return reposurl;
    }

    /**
     * Sets the URL for the author's repositories.
     *
     * @param value The repos URL as a {@code String}.
     */
    public void setReposurl(String value) {
        this.reposurl = value;
    }

    /**
     * Retrieves the URL for the author's following list.
     *
     * @return The following URL as a {@code String}.
     */
    public String getFollowingurl() {
        return followingurl;
    }

    /**
     * Sets the URL for the author's following list.
     *
     * @param value The following URL as a {@code String}.
     */
    public void setFollowingurl(String value) {
        this.followingurl = value;
    }

    /**
     * Retrieves the URL for the author's starred repositories.
     *
     * @return The starred URL as a {@code String}.
     */
    public String getStarredurl() {
        return starredurl;
    }

    /**
     * Sets the URL for the author's starred repositories.
     *
     * @param value The starred URL as a {@code String}.
     */
    public void setStarredurl(String value) {
        this.starredurl = value;
    }

    /**
     * Retrieves the login name of the author.
     *
     * @return The login name as a {@code String}.
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the login name of the author.
     *
     * @param value The login name as a {@code String}.
     */
    public void setLogin(String value) {
        this.login = value;
    }

    /**
     * Retrieves the URL for the author's followers list.
     *
     * @return The followers URL as a {@code String}.
     */
    public String getFollowersurl() {
        return followersurl;
    }

    /**
     * Sets the URL for the author's followers list.
     *
     * @param value The followers URL as a {@code String}.
     */
    public void setFollowersurl(String value) {
        this.followersurl = value;
    }

    /**
     * Retrieves the type of the author.
     *
     * @return The type as a {@code String}.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the author.
     *
     * @param value The type as a {@code String}.
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Retrieves the URL for the author.
     *
     * @return The URL as a {@code String}.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL for the author.
     *
     * @param value The URL as a {@code String}.
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Retrieves the URL for the author's subscriptions.
     *
     * @return The subscriptions URL as a {@code String}.
     */
    public String getSubscriptionsurl() {
        return subscriptionsurl;
    }

    /**
     * Sets the URL for the author's subscriptions.
     *
     * @param value The subscriptions URL as a {@code String}.
     */
    public void setSubscriptionsurl(String value) {
        this.subscriptionsurl = value;
    }

    /**
     * Retrieves the URL for the author's received events.
     *
     * @return The received events URL as a {@code String}.
     */
    public String getReceivedEventsurl() {
        return receivedEventsurl;
    }

    /**
     * Sets the URL for the author's received events.
     *
     * @param value The received events URL as a {@code String}.
     */
    public void setReceivedEventsurl(String value) {
        this.receivedEventsurl = value;
    }

    /**
     * Retrieves the URL for the author's avatar image.
     *
     * @return The avatar URL as a {@code String}.
     */
    public String getAvatarurl() {
        return avatarurl;
    }

    /**
     * Sets the URL for the author's avatar image.
     *
     * @param value The avatar URL as a {@code String}.
     */
    public void setAvatarurl(String value) {
        this.avatarurl = value;
    }

    /**
     * Retrieves the URL for the author's events.
     *
     * @return The events URL as a {@code String}.
     */
    public String getEventsurl() {
        return eventsurl;
    }

    /**
     * Sets the URL for the author's events.
     *
     * @param value The events URL as a {@code String}.
     */
    public void setEventsurl(String value) {
        this.eventsurl = value;
    }

    /**
     * Retrieves the URL for the author's GitHub profile.
     *
     * @return The GitHub profile URL as a {@code String}.
     */
    public String getHtmlurl() {
        return htmlurl;
    }

    /**
     * Sets the URL for the author's GitHub profile.
     *
     * @param value The GitHub profile URL as a {@code String}.
     */
    public void setHtmlurl(String value) {
        this.htmlurl = value;
    }

    /**
     * Retrieves whether the author is a site administrator.
     *
     * @return {@code true} if the author is a site administrator, otherwise {@code false}.
     */
    public boolean getSiteAdmin() {
        return siteAdmin;
    }

    /**
     * Sets whether the author is a site administrator.
     *
     * @param value {@code true} if the author is a site administrator, otherwise {@code false}.
     */
    public void setSiteAdmin(boolean value) {
        this.siteAdmin = value;
    }

    /**
     * Retrieves the unique identifier for the author.
     *
     * @return The unique identifier as a {@code long}.
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the author.
     *
     * @param value The unique identifier as a {@code long}.
     */
    public void setId(long value) {
        this.id = value;
    }

    /**
     * Retrieves the gravatar ID of the author.
     *
     * @return The gravatar ID as a {@code String}.
     */
    public String getGravatarid() {
        return gravatarid;
    }

    /**
     * Sets the gravatar ID of the author.
     *
     * @param value The gravatar ID as a {@code String}.
     */
    public void setGravatarid(String value) {
        this.gravatarid = value;
    }

    /**
     * Retrieves the node ID for the author.
     *
     * @return The node ID as a {@code String}.
     */
    public String getNodeid() {
        return nodeid;
    }

    /**
     * Sets the node ID for the author.
     *
     * @param value The node ID as a {@code String}.
     */
    public void setNodeid(String value) {
        this.nodeid = value;
    }

    /**
     * Retrieves the URL for the author's organizations.
     *
     * @return The organizations URL as a {@code String}.
     */
    public String getOrganizationsurl() {
        return organizationsurl;
    }

    /**
     * Sets the URL for the author's organizations.
     *
     * @param value The organizations URL as a {@code String}.
     */
    public void setOrganizationsurl(String value) {
        this.organizationsurl = value;
    }
}
