package com.maogm.wanquribao.Module;

import com.orm.SugarRecord;

/**
 * Use to record if a issue is requested, avoid request again
 * @author Guangming Mao
 */
public class IssueRecord extends SugarRecord<PostModel> {
    public int number;
    public boolean requested;

    public IssueRecord() {
    }

    public IssueRecord(int number, boolean requested) {
        this.number = number;
        this.requested = requested;
    }
}
