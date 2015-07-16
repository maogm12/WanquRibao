package com.maogm.wanquribao.Module;

import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

/**
 * @author Guangming Mao
 */
public class Issue extends SugarRecord<Issue> {
    public String date;
    @SerializedName("created_at")
    public int createdAt;
    public int number;
    public String summary;

    public Issue() {
    }

    public Issue(String date, int createdAt, int number, String summary) {
        this.date = date;
        this.createdAt = createdAt;
        this.number = number;
        this.summary = summary;
    }
}
