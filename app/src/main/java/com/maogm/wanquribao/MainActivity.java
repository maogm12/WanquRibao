package com.maogm.wanquribao;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;


public class MainActivity extends AppCompatActivity
        implements OnShareListener, IssuesFragment.OnIssueSelectedListener {

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

        navigationDrawerSelected(0);
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
        FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                // latest issue
                IssueFragment issueFragment = IssueFragment.newInstance();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, issueFragment)
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

    /**
     * Open a issue
     * @param number issue number of the issue
     */
    public void openIssue(int number) {
        if (number < 0) {
            number = -1;
        }

        LogUtil.d(TAG, "open issue number: " + number);

        FragmentManager fragmentManager = getFragmentManager();
        IssueFragment issueFragment = IssueFragment.newInstance(number);
        fragmentManager.beginTransaction()
                .replace(R.id.container, issueFragment)
                .addToBackStack(null)
                .commit();
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
       // if (!mNavigationDrawerFragment.isDrawerOpen()) {
        if (true) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
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
        return super.onCreateOptionsMenu(menu);
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

    private long exitTime = 0;
    public void ExitApp()
    {
        if ((System.currentTimeMillis() - exitTime) > 2000)
        {
            Toast.makeText(this, "再按一次退出程序",  Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else
        {
            finish();
        }

    }
}
