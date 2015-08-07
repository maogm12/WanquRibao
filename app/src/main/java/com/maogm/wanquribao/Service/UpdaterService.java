package com.maogm.wanquribao.Service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

/**
 * The service to check for update
 * @author Guangming Mao
 */
public class UpdaterService extends IntentService {

    private static final String TAG = "UpdaterService";
    private static final String keyLastCheckTime = "LAST_CHECK_TIME";
    private long lastCheckTimeStamp = 0;

    public UpdaterService() {
        super("UpdaterService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int currentVersionCode;
        boolean updateAvailable = false;
        try {
            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            LogUtil.e(TAG, "package name not found");
            return;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.contains(keyLastCheckTime)) {
            lastCheckTimeStamp = pref.getLong(keyLastCheckTime, 0);
        }

        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        final long millisecondsInADay = 86400000;  // 24 * 3600 * 1000
        if (lastCheckTimeStamp + 3 * millisecondsInADay > now) {
            LogUtil.d(TAG, "last time check is " + (now - lastCheckTimeStamp) + " ms ago");
            // check every 3 days
            // return;
        }

        LogUtil.d(TAG, "check for update");
        pref.edit().putLong(keyLastCheckTime, cal.getTimeInMillis()).apply();

        try {
            URL url = new URL(Constant.versionUrl);
            URLConnection urlConnection = url.openConnection();

            // read content
            StringBuilder content = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;

            // read from the urlconnection via the bufferedreader
            while ((line = bufferedReader.readLine()) != null)
            {
                content.append(line).append("\n");
            }
            bufferedReader.close();

            // parse content to see if it is a new number
            int newVersionCode = Integer.parseInt(content.toString());
            LogUtil.d(TAG, "new version code: " + newVersionCode);
            if (newVersionCode > currentVersionCode) {
                updateAvailable = true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            LogUtil.e(TAG, "version check url error: " + Constant.versionUrl);
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.e(TAG, "connection error when open url: " + Constant.versionUrl);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            LogUtil.e(TAG, "not a valid number");
        }

        if (updateAvailable) {
            sendUpdateCheckerMessage(updateAvailable);
        }
    }

    private void sendUpdateCheckerMessage(boolean updateAvailable) {
        LogUtil.d(TAG, "send update checker message");
        Intent intent = new Intent(Constant.NAME_INTENT_UPDATE_CHECKER);
        intent.putExtra(Constant.KEY_UPDATE_AVAILABLE, updateAvailable);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
