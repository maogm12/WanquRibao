package com.maogm.wanquribao;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;

public class WebViewActivity extends Activity {
    private static final String TAG = "WebViewActivity";

    private WebView webView;
    private ImageButton ibtnShare;

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
        ibtnShare = (ImageButton) findViewById(R.id.btn_share);
        ImageButton ibtnExit = (ImageButton) findViewById(R.id.btn_exit);

        // exit
        ibtnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebViewActivity.this.finish();
            }
        });


        // @todo share
    }

    @Override
    protected void onResume() {
        super.onResume();

        // load data
        Bundle bundle = getIntent().getExtras();
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
}
