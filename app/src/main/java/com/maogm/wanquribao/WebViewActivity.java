package com.maogm.wanquribao;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";

    private ProgressBar progressBar;
    private WebView webView;
    private SwipeRefreshLayout swipeView;

    private String html;
    private String url;
    private String shareSubject;
    private String shareBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load data
        Bundle bundle = getIntent().getExtras();
        shareSubject = bundle.getString(Constant.KEY_SHARE_SUBJECT);
        shareBody = bundle.getString(Constant.KEY_SHARE_BODY);
        html = bundle.getString(Constant.KEY_HTML);
        url = bundle.getString(Constant.KEY_URL);

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

        // progress bar
        progressBar = (ProgressBar) findViewById(R.id.progress);
        if (isLoadOnline()) {
            // change color
            progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }

        webView = (WebView) findViewById(R.id.fullscreen_content);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                // load from local
                if (isLoadLocal()) {
                    return;
                }

                // hide the swipe view
                if (newProgress > 50 && swipeView.isRefreshing()) {
                    swipeView.setRefreshing(false);
                }

                // update progress bar
                if (newProgress == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setProgress(newProgress);
                }
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);

        // swipte to refresh
        swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipeView.setColorSchemeResources(R.color.main);
        if (html == null || url != null) {
            swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeView.setRefreshing(true);
                    webView.stopLoading();
                    webView.reload();
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
        }

        FloatingActionButton ibtnShare = (FloatingActionButton) findViewById(R.id.btn_share);
        FloatingActionButton ibtnExit = (FloatingActionButton) findViewById(R.id.btn_exit);

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
                    shareBody = getString(R.string.recommend_app) + getString(R.string.app_desc) +
                            getString(R.string.via_app, Constant.playUrl);
                }
                shareText(shareSubject, shareBody);
            }
        });
    }

    private boolean isLoadLocal() {
        return html != null;
    }

    private boolean isLoadOnline() {
        return html == null && url != null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (html != null) {
            setContent(html);
            return;
        }

        if (url != null) {
            setUrl(url);
            return;
        }

        LogUtil.e(TAG, "no data and not url provided");
    }

    public void setContent(String html) {
        if (html == null) {
            return;
        }

        if (webView == null) {
            LogUtil.e(TAG, "can not find the webview");
            return;
        }

        LogUtil.i(TAG, "load data from html");
        webView.stopLoading();
        webView.loadData(html, "text/html", "utf-8");
    }

    public void setUrl(String url) {
        if (url == null) {
            return;
        }

        if (webView == null) {
            LogUtil.e(TAG, "can not find the webview");
            return;
        }

        LogUtil.i(TAG, "load data from url" + url);
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
