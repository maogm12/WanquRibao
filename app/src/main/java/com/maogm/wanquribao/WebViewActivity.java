package com.maogm.wanquribao;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import com.maogm.wanquribao.Utils.Constant;

public class WebViewActivity extends Activity {
    private static final String TAG = "WebViewActivity";

    private AnimatingProgressBar progressBar;

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

        progressBar = (AnimatingProgressBar) findViewById(R.id.progress);
        // progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

        webView = (WebView) findViewById(R.id.fullscreen_content);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setProgress(100);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);

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
                if (shareBody == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(getString(R.string.recommend_app))
                            .append(getString(R.string.app_desc))
                            .append(getString(R.string.via_app, Constant.playUrl));
                    shareBody = sb.toString();
                }
                shareText(shareSubject, shareBody);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        progressBar.setProgress(90);

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
