package com.maogm.wanquribao;


import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import com.maogm.wanquribao.Utils.Constant;

/**
 * About page
 * @author Guangming Mao
 */
public class AboutFragment extends PreferenceFragment {

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
    }

    private void bindPreferenceWithUrl(String key, String url) {
        Preference pref = findPreference(key);
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
        if (url == null) {
            return;
        }

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_URL, url);
        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }

    private void openEmail(String email) {
        if (email  == null) {
            return;
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
    }
}
