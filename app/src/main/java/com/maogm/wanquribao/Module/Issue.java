package com.maogm.wanquribao.Module;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

/**
 * @author Guangming Mao
 */
public class Issue extends SugarRecord<Issue> implements Parcelable {
    public String date;
    @SerializedName("created_at")
    public long createdAt;
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

    public Issue(Parcel source) {
        String[] strs = new String[2];
        source.readStringArray(strs);
        date = strs[0];
        summary = strs[1];
        createdAt = source.readLong();
        number = source.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{
                date, summary
        });
        dest.writeLong(createdAt);
        dest.writeInt(number);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Issue createFromParcel(Parcel source) {
            return new Issue(source);
        }

        @Override
        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };
}
