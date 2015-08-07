package com.maogm.wanquribao;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Listener.PostManager;
import com.maogm.wanquribao.Listener.WebViewManager;
import com.maogm.wanquribao.Service.UpdaterService;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;


public class MainActivity extends AppCompatActivity
        implements OnShareListener, IssuesFragment.OnIssueSelectedListener, WebViewManager,
        PostManager {

    private static final String TAG = "MainActivity";

    /**F
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DrawerLayout mDrawerLayout;

    private CharSequence title;

    private ShareActionProvider mShareActionProvider;
    private Intent shareIntent;
    private Intent defaultShareIntent;

    private MenuItem shareItem;
    private boolean canShare = true;

    private int currentTab = -1;

    /**
     * receive the update checker's message
     */
    private BroadcastReceiver updateCheckerMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // if update available
            boolean updateAvailable = intent.getBooleanExtra(Constant.KEY_UPDATE_AVAILABLE, false);
            LogUtil.d(TAG, "show update dialog");
            if (updateAvailable) {
                // tell user to update
                LogUtil.d(TAG, "need update");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.new_version_available);
                builder.setMessage(R.string.go_to_market);
                builder.setPositiveButton(R.string.confirm_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogUtil.d(TAG, "go to market to download");
                        openDetailInMarket();
                    }
                });
                builder.setNegativeButton(R.string.next_time, null);
                builder.show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolBarSetUp();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView =
                (NavigationView) findViewById(R.id.nv_main_navigation);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(Constant.KEY_CURRENT_TAB);
            return;
        }

        navigationDrawerSelected(0);

        // check for update
        LocalBroadcastManager.getInstance(this).registerReceiver(updateCheckerMessageReceiver, new IntentFilter(Constant.NAME_INTENT_UPDATE_CHECKER));
        Intent updateCheckIntent = new Intent(this, UpdaterService.class);
        startService(updateCheckIntent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        LogUtil.d(TAG, "save current selected tab: " + String.valueOf(currentTab));
        outState.putInt(Constant.KEY_CURRENT_TAB, currentTab);
    }

    private void toolBarSetUp() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        switch (menuItem.getItemId()) {
                            case R.id.nav_home:
                                navigationDrawerSelected(0);
                                break;
                            case R.id.nav_menu2:
                                navigationDrawerSelected(1);
                                break;
                            case R.id.nav_menu3:
                                navigationDrawerSelected(2);
                                break;
                            case R.id.nav_about:
                                navigationDrawerSelected(3);
                                break;
                            default:
                                break;
                        }
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    private void navigationDrawerSelected(int position) {
        currentTab = position;
        FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                // latest issue
                PostsFragment postsFragment = PostsFragment.newInstance();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, postsFragment)
                        .commit();
                break;
            case 1:
                // past issues
                IssuesFragment issuesFragment = IssuesFragment.newInstance();
                issuesFragment.setOnIssueSelectedListener(this);
                fragmentManager.beginTransaction()
                        .replace(R.id.container, issuesFragment)
                        .commit();
                break;
            case 2:
                // random post
                RandomPostFragment randomPostFragment = new RandomPostFragment();
                randomPostFragment.setOnShareListener(this);
                fragmentManager.beginTransaction()
                        .replace(R.id.container, randomPostFragment)
                        .commit();
                break;
            case 3:
                // about
                AboutFragment aboutFragment = new AboutFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, aboutFragment)
                        .commit();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return;
        }

        ExitApp();
    }

    public void updateTitle(CharSequence title) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        this.title = title;
        actionBar.setTitle(title);
    }

    public void restoreTitle() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        actionBar.setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        restoreTitle();

        // Locate MenuItem with ShareActionProvider
        shareItem = menu.findItem(R.id.action_share);
        onGlobalShareEnabled(canShare);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        setShareIntent(shareIntent);

        return true;
    }

    // Call to update the share intent
    public void setShareIntent(Intent shareIntent) {
        this.shareIntent = shareIntent;

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    public void restoreShareIntent() {
        this.shareIntent = getDefaultShareIntent();

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.action_share) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Intent getDefaultShareIntent() {
        if (defaultShareIntent != null) {
            return defaultShareIntent;
        }

        defaultShareIntent = new Intent();
        defaultShareIntent.setAction(Intent.ACTION_SEND);
        defaultShareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_me));
        defaultShareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.recommend_app) +
                getString(R.string.app_desc) + getString(R.string.via_app, Constant.playUrl));
        defaultShareIntent.setType("text/plain");
        return defaultShareIntent;
    }

    public void onShareText(String subject, String body) {
        LogUtil.d(TAG, "share text, subject: " + subject + " body: " + body);

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_me)));
    }

    @Override
    public void onGlobalShareContentChanged(String subject, String body) {
        if (subject == null || body == null) {
            LogUtil.d(TAG, "global share changed to default");
            onRestoreGlobalShare();
            return;
        }

        LogUtil.d(TAG, "global share changed to: subject: " + subject + " body: " + body);

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
        setShareIntent(sharingIntent);
    }

    @Override
    public void onRestoreGlobalShare() {
        setShareIntent(getDefaultShareIntent());
    }

    @Override
    public void onGlobalShareEnabled(boolean enable) {
        if (shareItem != null) {
            shareItem.setVisible(enable);
        }
        canShare = enable;
    }

    @Override
    public void openUrl(String url, String subject, String body) {
        if (url == null) {
            return;
        }

        LogUtil.d(TAG, "openUrl, url: " + url + " subject: " + subject + " body: " + body);

        Intent webViewIntent = new Intent(this, WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_URL, url);
        if (subject == null) {
            subject = getString(R.string.share_link);
        }
        bundle.putString(Constant.KEY_SHARE_SUBJECT, subject);
        if (body != null) {
            bundle.putString(Constant.KEY_SHARE_BODY, body);
        }
        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }

    @Override
    public void openHtml(String html, String subject, String body) {
        if (html == null) {
            return;
        }

        LogUtil.d(TAG, "openHtml, subject: " + subject + " body: " + body);

        Intent webViewIntent = new Intent(this, WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_HTML, html);
        if (subject == null) {
            subject = getString(R.string.share_link);
        }
        bundle.putString(Constant.KEY_SHARE_SUBJECT, subject);
        if (body != null) {
            bundle.putString(Constant.KEY_SHARE_BODY, body);
        }
        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }

    private long exitTime = 0;
    public void ExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, "再按一次退出程序",  Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    /**
     * Open posts by number
     * @param number number of issue
     */
    @Override
    public void openPostsByIssueNumber(int number) {
        if (number < 0) {
            number = -1;
        }

        LogUtil.d(TAG, "open issue number: " + number);

        FragmentManager fragmentManager = getFragmentManager();
        PostsFragment postsFragment = PostsFragment.newInstance(number);
        fragmentManager.beginTransaction()
                .replace(R.id.container, postsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openPostsByTag(String tag) {
        if (tag == null) {
            return;
        }

        LogUtil.d(TAG, "open tag: " + tag);

        FragmentManager fragmentManager = getFragmentManager();
        PostsFragment postsFragment = PostsFragment.newInstance(tag);
        fragmentManager.beginTransaction()
                .replace(R.id.container, postsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openIssue(int number) {
        openPostsByIssueNumber(number);
    }

    private void openDetailInMarket() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + getPackageName()));
        startActivity(intent);
    }

}
