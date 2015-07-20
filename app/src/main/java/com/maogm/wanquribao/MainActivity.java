package com.maogm.wanquribao;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;

import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Utils.Constant;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, OnShareListener, IssuesFragment.OnIssueSelectedListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

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

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
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
        }
    }

    /**
     * Open a issue
     * @param number issue number of the issue
     */
    public void openIssue(int number) {
        if (number < 0) {
            number = -1;
        }

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
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
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

        if (id == R.id.action_share) {
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
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.recommend_app))
                .append(getString(R.string.app_desc))
                .append(getString(R.string.via_app, Constant.playUrl));
        defaultShareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        defaultShareIntent.setType("text/plain");
        return defaultShareIntent;
    }

    public void onShareText(String subject, String body) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_me)));
    }

    @Override
    public void onGlobalShareContentChanged(String subject, String body) {
        if (subject == null || body == null) {
            onRestoreGlobalShare();
            return;
        }

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
}
