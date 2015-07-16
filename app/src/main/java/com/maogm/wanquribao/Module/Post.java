package com.maogm.wanquribao.Module;

import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

import java.util.List;

/**
 * @author Guangming Mao
 */
public class Post extends SugarRecord<Post> {
    @SerializedName("url_domain")
    public String urlDomain;
    public String picture;
    public List<String> tags;
    public String url;
    @SerializedName("readable_title")
    public String readableTitle;
    @SerializedName("created_at")
    public int createdAt;
    public String title;
    public int id;
    public String content;
    @SerializedName("issue_id")
    public int issueId;
    public String summary;
    public String slug;
    @SerializedName("readable_article")
    public String readableArticle;

    public Post() {
    }

    public Post(String urlDomain, String picture, List<String> tags, String url, String readableTitle,
                int createdAt, String title, int id, String content, int issueId, String summary,
                String slug, String readableArticle) {
        this.urlDomain = urlDomain;
        this.picture = picture;
        this.tags = tags;
        this.url = url;
        this.readableTitle = readableTitle;
        this.createdAt = createdAt;
        this.title = title;
        this.id = id;
        this.content = content;
        this.issueId = issueId;
        this.summary = summary;
        this.slug = slug;
        this.readableArticle = readableArticle;
    }
}
