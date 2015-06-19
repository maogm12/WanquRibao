package com.maogm.wanquribao.Module;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

public class DbHelper {
    private WanquRibaoDbHelper dbHelper;
    private SQLiteDatabase db;

    public DbHelper(Context context) {
        dbHelper = new WanquRibaoDbHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void writeIssue(Issue issue) {

    }

    public void writeIssueItem(IssueItem item, Issue issue) {
        db.beginTransaction();  //开始事务
        try {
            String tags = join(item.tags, ",");
            db.execSQL("INSERT INTO issueItem VALUES(null, ?, ?, ?, ?, ?, ?)",
                    new Object[]{issue._id});
            db.setTransactionSuccessful();  //设置事务成功完成
        } finally {
            db.endTransaction();    //结束事务
        }
    }

    public static String join(List<String> array, String delimiter) {
        if (delimiter == null) {
            delimiter = "";
        }

        StringBuilder result = new StringBuilder();
        boolean firstItem = true;
        for (String item: array) {
            if (!firstItem) {
                result.append(delimiter);
            }
            result.append(item);
            firstItem = false;
        }
        return result.toString();
    }
}

/**
 * DbHelper class
 * @author Guangming Mao
 */
class WanquRibaoDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "WanquRibaoDbHelper";
    private static final String DATABASE_NAME = "wanqu.db";
    private static final int DATABASE_VERSION = 1;

    public WanquRibaoDbHelper(Context context) {
        //CursorFactory设置为null,使用默认值
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //数据库第一次被创建时onCreate会被调用
    @Override
    public void onCreate(SQLiteDatabase db) {
        createIssueTable(db);
        createIssueItemTable(db);
    }

    //如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade");

    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    private void createIssueTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS issue(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "number INTEGER, " +
                "time DATE)");
    }

    private void createIssueItemTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS issueItem (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "iid INTEGER, " +   // issue id
                "title VARCHAR(500), " +
                "link VARCHAR(500), " +
                "originalLink VARCHAR(500), " +
                "brief TEXT, " +
                "tags VARCHAR(500))");
    }
}