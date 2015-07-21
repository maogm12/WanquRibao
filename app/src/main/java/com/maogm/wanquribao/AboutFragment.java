package com.maogm.wanquribao;


import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toast;

import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;

/**
 * About page
 * @author Guangming Mao
 */
public class AboutFragment extends PreferenceFragment {
    private static final String TAG = "AboutFragment";
    private int clickTimes = 0;

    public AboutFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((MainActivity)getActivity()).restoreShareIntent();
    }

    @Override
    public void onStart() {
        super.onStart();
        clickTimes = 0;

        // bind link or email to preference
        bindPreferenceWithUrl(Constant.KEY_OFFICIAL_SITE, Constant.LINK_OFFICIAL_SITE);
        bindPreferenceWithUrl(Constant.KEY_ABOUT_WANQU, Constant.LINK_ABOUT_WANQU);
        bindPreferenceWithUrl(Constant.KEY_WEIBO, Constant.LINK_WEIBO);
        bindPreferenceWithUrl(Constant.KEY_WEICHAT, Constant.LINK_WEICHAT);
        bindPreferenceWithUrl(Constant.KEY_TWITTER, Constant.LINK_TWITTER);
        bindPreferenceWithUrl(Constant.KEY_FACEBOOK, Constant.LINK_FACEBOOK);
        bindPreferenceWithUrl(Constant.KEY_AUTHOR, Constant.LINK_AUTHOR);
        bindPreferenceWithUrl(Constant.KEY_MY_WEIBO, Constant.LINK_MY_WEIBO);

        bindPreferenceWithEmail(Constant.KEY_WANQU_EMAIL, Constant.WANQU_EMAIL);
        bindPreferenceWithEmail(Constant.KEY_MY_EMAIL, Constant.MY_EMAIL);

        // version, 5 clicks to enable debug
        Preference pref = findPreference(Constant.KEY_VERSION);
        if (pref != null) {
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickTimes += 1;
                    if (clickTimes >= 5) {
                        LogUtil.DEBUG = !LogUtil.DEBUG;
                        clickTimes = 0;
                        String msg = LogUtil.DEBUG ? "Enable debug" : "Disable debug";
                        LogUtil.d(TAG, msg);
                        if (!isAdded()) {
                            return false;
                        }

                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
        }

        // update title
        if (isAdded()) {
            ((MainActivity)getActivity()).updateTitle(getString(R.string.title_section_about));
        }
    }

    private void bindPreferenceWithUrl(String key, String url) {
        LogUtil.d(TAG, "Click key: " + key + " to open " + url);
        Preference pref = findPreference(key);
        if (pref == null) {
            LogUtil.e(TAG, "Preference with key: " + key + " no exists");
            return;
        }
        final String theUrl = url;
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openUrl(theUrl);
                return true;
            }
        });
    }

    private void bindPreferenceWithEmail(String key, String email) {
        Preference pref = findPreference(key);
        final String theEmail = email;
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openEmail(theEmail);
                return true;
            }
        });
    }

    private void openUrl(String url) {
        if (url == null || !isAdded()) {
            return;
        }

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_URL, url);
        bundle.putString(Constant.KEY_SHARE_SUBJECT, getString(R.string.share_me));
        bundle.putString(Constant.KEY_SHARE_BODY, getString(R.string.recommend_app) +
                getString(R.string.app_desc) + getString(R.string.via_app, Constant.playUrl));
        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }

    private void openEmail(String email) {
        if (email  == null || !isAdded()) {
            return;
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
    }
}
