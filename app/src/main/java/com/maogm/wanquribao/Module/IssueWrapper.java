package com.maogm.wanquribao.Module;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author Guangming Mao
 */
public class IssueWrapper {
    @SerializedName("oldest_timestamp")
    public long oldestTimestamp;
    public List<Issue> issues;
}
