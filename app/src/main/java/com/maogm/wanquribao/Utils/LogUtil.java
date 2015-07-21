package com.maogm.wanquribao.Utils;

import android.util.Log;

/**
 * LogUtil
 *
 */
public class LogUtil {
    public static boolean DEBUG = false;

    public static void v(String tag, String msg) {
        if (DEBUG) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {
            Log.w(tag, msg);
        }
    }

    public static void wtf(String tag, String msg) {
        if (DEBUG) {
            Log.wtf(tag, msg);
        }
    }
}
