package com.maogm.wanquribao.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * IssueItem
 * @author Guangming Mao
 */
public class IssueItem {
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
}
