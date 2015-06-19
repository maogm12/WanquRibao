package com.maogm.wanquribao.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * IssueItem
 * @author Guangming Mao
 */
public class IssueItem {
    public int _id;
    public String title;
    public String link;
    public String originalLink;
    public String brief;
    public List<String> tags;


    public IssueItem() {
        title = "";
        link = "http://wanqu.co";
        originalLink = "";
        brief = "";
        tags = new ArrayList<>();
    }

    public IssueItem(String title, String link, String originalLink, String brief, List<String> tags) {
        this.title = title;
        this.link = link;
        this.originalLink = originalLink;
        this.brief = brief;
        this.tags = tags;
    }
}
