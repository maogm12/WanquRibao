package com.maogm.wanquribao;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";

    private ProgressBar progressBar;
    private ObservableWebView webView;
    private SwipeRefreshLayout swipeView;

    private enum LoadType {
        FROM_LOCAL, FROM_REMOTE
    }
    private LoadType loadType;

    private String html;
    private String url;
    private String shareSubject;
    private String shareBody;

    private String pageUrl;

    private FloatingActionsMenu fabMenu;
    private Animator currentAnimator;
    private int shortAnimationDuration;

    private static final String htmlPrefix =
"<style> img, table { max-width: 100%; height: auto; }</style>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load data
        Bundle bundle = getIntent().getExtras();

        shareSubject = bundle.getString(Constant.KEY_SHARE_SUBJECT);
        shareBody = bundle.getString(Constant.KEY_SHARE_BODY);
        html = bundle.getString(Constant.KEY_HTML);
        url = bundle.getString(Constant.KEY_URL);
        pageUrl = url;
        if (url != null) {
            loadType = LoadType.FROM_REMOTE;
        } else {
            loadType = LoadType.FROM_LOCAL;
        }

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
        progressBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

        // floating action buttons
        final FloatingActionButton fabOpenBrowser = (FloatingActionButton) findViewById(R.id.btn_open_in_browser);
        final FloatingActionButton fabShare = (FloatingActionButton) findViewById(R.id.btn_share);
        final FloatingActionButton fabExit = (FloatingActionButton) findViewById(R.id.btn_exit);
        if (loadType == LoadType.FROM_LOCAL) {
            fabOpenBrowser.setVisibility(View.GONE);
        }

        webView = (ObservableWebView) findViewById(R.id.fullscreen_content);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (URLUtil.isValidUrl(url)) {
                    pageUrl = url;
                    fabOpenBrowser.setVisibility(View.VISIBLE);
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

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
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // swipe to refresh
        swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipeView.setColorSchemeResources(R.color.main);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (pageUrl == null) {
                    swipeView.setRefreshing(false);
                    return;
                }
                swipeView.setRefreshing(true);
                webView.stopLoading();
                webView.reload();
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        // menu
        fabMenu = (FloatingActionsMenu) findViewById(R.id.actions);
        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(fabMenu, "alpha", .1f, 1f);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(shortAnimationDuration);
        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(fabMenu, "alpha",  1f, .1f);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(shortAnimationDuration);

        final AnimatorSet fadeInSet = new AnimatorSet();
        fadeInSet.play(fadeIn);
        fadeInSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                fabMenu.setAlpha(0);
                fabMenu.setVisibility(View.VISIBLE);
            }
        });

        final AnimatorSet fadeOutSet = new AnimatorSet();
        fadeOutSet.play(fadeOut);
        fadeOutSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fabMenu.setVisibility(View.GONE);
            }
        });
        fadeInSet.start();

        // set btn visible or not
        webView.setOnScrollChangedCallback(new ObservableWebView.OnScrollChangedCallback() {
            @Override
            public void onScroll(int l, int t, int oldl, int oldt) {
                if (t < oldt) {
                    fabMenu.collapse();
                    if (fadeInSet.isRunning() || fabMenu.getVisibility() == View.VISIBLE) {
                        return;
                    }
                    fadeOutSet.cancel();
                    fadeInSet.start();
                } else {
                    fabMenu.collapse();
                    if (fadeOutSet.isRunning() || fabMenu.getVisibility() == View.GONE) {
                        return;
                    }
                    fadeInSet.cancel();
                    fadeOutSet.start();
                }
            }
        });

        // open browser
        fabOpenBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pageUrl == null) {
                    Toast.makeText(WebViewActivity.this, R.string.no_url, Toast.LENGTH_SHORT).show();
                }
                fabMenu.collapse();

                // open browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    // ignore
                }
            }
        });

        // exit
        fabExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabMenu.collapse();
                WebViewActivity.this.finish();
            }
        });

        // share
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareBody == null) {
                    shareBody = getString(R.string.recommend_app) + getString(R.string.app_desc) +
                            getString(R.string.via_app, Constant.playUrl);
                }
                fabMenu.collapse();
                shareText(shareSubject, shareBody);
            }
        });
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
        // specify the charset of the content
        webView.loadData(htmlPrefix + html, "text/html; charset=utf-8", "utf-8");
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
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
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
