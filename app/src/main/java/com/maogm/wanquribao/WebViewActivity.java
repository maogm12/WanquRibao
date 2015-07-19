package com.maogm.wanquribao;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;

import com.maogm.wanquribao.Utils.Constant;

public class WebViewActivity extends Activity {
    private static final String TAG = "WebViewActivity";

    private WebView webView;
    private String shareSubject;
    private String shareBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        // make it fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // hide action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        webView = (WebView) findViewById(R.id.fullscreen_content);
        ImageButton ibtnShare = (ImageButton) findViewById(R.id.btn_share);
        ImageButton ibtnExit = (ImageButton) findViewById(R.id.btn_exit);

        // exit
        ibtnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebViewActivity.this.finish();
            }
        });


        // share
        ibtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareText(shareSubject, shareBody);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // load data
        Bundle bundle = getIntent().getExtras();

        // share
        shareSubject = bundle.getString(Constant.KEY_SHARE_SUBJECT);
        shareBody = bundle.getString(Constant.KEY_SHARE_BODY);

        String html = bundle.getString(Constant.KEY_HTML);
        if (html != null) {
            setContent(html);
            return;
        }

        String url = bundle.getString(Constant.KEY_URL);
        if (url != null) {
            setUrl(url);
            return;
        }

        Log.e(TAG, "no data and not url provided");
    }

    public void setContent(String html) {
        if (html == null) {
            return;
        }

        if (webView == null) {
            Log.e(TAG, "can not find the webview");
            return;
        }

        Log.i(TAG, "load data from html");
        webView.stopLoading();
        webView.loadData(html, "text/html", "utf-8");
    }

    public void setUrl(String url) {
        if (url == null) {
            return;
        }

        if (webView == null) {
            Log.e(TAG, "can not find the webview");
            return;
        }

        Log.i(TAG, "load data from url" + url);
        webView.stopLoading();
        webView.loadUrl(url);
    }

    public void shareText(String subject, String body) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_me)));
    }
}
