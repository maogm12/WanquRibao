package com.maogm.wanquribao.Listener;

/**
 * Issue manager, used to open issue
 */
public interface PostManager {
    void openPostsByIssueNumber(int number);
    void openPostsByTag(String tag);
}
