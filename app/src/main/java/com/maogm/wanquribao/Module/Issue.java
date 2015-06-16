package com.maogm.wanquribao.Module;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Issue
 * @author Guangming Mao
 */
public class Issue {
    public List<IssueItem> issues;
    public int number;
    public Date time;

    public Issue() {
        issues = new ArrayList<IssueItem>();
        number = -1;
        time = new Date();
    }
}
