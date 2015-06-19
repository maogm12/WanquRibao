package com.maogm.wanquribao.Module;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Issue
 * @author Guangming Mao
 */
public class Issue {
    public int _id;
    public int number;
    public Date time;
    public List<IssueItem> issues;

    public Issue() {
        number = -1;
        time = new Date();
        issues = new ArrayList<IssueItem>();
    }

    public Issue(int number, Date time, List<IssueItem> issues) {
        this.number = number;
        this.time = time;
        this.issues = issues;
    }
}
