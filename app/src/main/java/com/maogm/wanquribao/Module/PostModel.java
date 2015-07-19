package com.maogm.wanquribao.Module;

import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * hack the SugarRecord
 * @author Guangming Mao
 */
public class PostModel extends SugarRecord<PostModel> {
    public String date;
    public int number;

    public String urlDomain;
    public String picture;
    public String tags;
    public String url;
    public String readableTitle;
    public long createdAt;
    public String title;
    public int id;
    public String content;
    public int issueId;
    public String summary;
    public String slug;
    public String readableArticle;

    public PostModel() {
    }

    public PostModel(String date, int number, String urlDomain, String picture, String tags, String url,
                     String readableTitle, long createdAt, String title, int id, String content,
                     int issueId, String summary, String slug, String readableArticle) {
        this.date = date;
        this.number = number;
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

    public PostModel(Post post, String date, int number) {
        // build the tags
        String tags = "";
        if (post.tags != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < post.tags.size(); ++i) {
                if (i == 0) {
                    sb.append(post.tags.get(i));
                } else {
                    sb.append(',').append(post.tags.get(i));
                }
            }
            tags = new String(sb);
        }

        this.date = date;
        this.number = number;
        this.urlDomain = post.urlDomain;
        this.picture = post.picture;
        this.tags = tags;
        this.url = post.url;
        this.readableTitle = post.readableTitle;
        this.createdAt = post.createdAt;
        this.title = post.title;
        this.id = post.id;
        this.content = post.content;
        this.issueId = post.issueId;
        this.summary = post.summary;
        this.slug = post.slug;
        this.readableArticle = post.readableArticle;
    }

    public Post getPost() {
        List<String> tags = new ArrayList<>();
        String[] arrTags = this.tags.split(",");
        Collections.addAll(tags, arrTags);

        return new Post(this.urlDomain, this.picture, tags, this.url, this.readableTitle,
                this.createdAt, this.title, this.id, this.content, this.issueId, this.summary,
                this.slug, this.readableArticle);
    }
}
