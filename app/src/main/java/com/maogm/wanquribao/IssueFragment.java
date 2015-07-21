package com.maogm.wanquribao;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Module.IssueResult;
import com.maogm.wanquribao.Module.Post;
import com.maogm.wanquribao.Module.PostModel;
import com.maogm.wanquribao.Module.PostWrapper;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;
import com.maogm.wanquribao.Utils.NetworkUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A fragment containing issue
 * @author Guangming Mao
 */
public class IssueFragment extends Fragment implements Response.Listener<IssueResult>, Response.ErrorListener {

    private static final String TAG = "IssueFragment";

    private static final String ISSUE_NUMBER = "issue_number";

    private String date;
    private int number = -1;
    private List<Post> posts;
    private ListView listPost;
    private PostAdapter postAdapter;
    private SwipeRefreshLayout swipeView;

    private OnShareListener shareListener;

    // request queue
    private RequestQueue postQueue;

    /**
     * Returns a new instance of this fragment for the given issue number.
     */
    public static IssueFragment newInstance(int number) {
        IssueFragment fragment = new IssueFragment();
        Bundle args = new Bundle();
        args.putInt(ISSUE_NUMBER, number);
        fragment.setArguments(args);
        return fragment;
    }

    public static IssueFragment newInstance() {
        return newInstance(-1);
    }

    public IssueFragment() {
        LogUtil.d(TAG, "new issueFragment");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnShareListener) {
            shareListener = (OnShareListener) activity;
            shareListener.onGlobalShareEnabled(true);
        } else {
            throw new IllegalArgumentException("activity must implement OnShareListener");
        }

        postQueue = Volley.newRequestQueue(activity);

        LogUtil.d(TAG, "onAttach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // list
        listPost = (ListView) view.findViewById(R.id.post_list);
        // swipeView
        swipeView = (SwipeRefreshLayout) view.findViewById(R.id.swipe);

        postAdapter = new PostAdapter();
        listPost.setAdapter(postAdapter);
        listPost.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            int mPosition = 0;
            int mOffset = 0;

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (swipeView == null) {
                    return;
                }

                int position = listPost.getFirstVisiblePosition();
                View v = listPost.getChildAt(position);
                int offset = (v == null) ? 0 : v.getTop();

                // disable swipe view when scrolled down
                if (mPosition < position || mPosition == position && mOffset < offset) {
                    swipeView.setEnabled(false);
                } else {
                    swipeView.setEnabled(true);
                }
            }
        });

        // swipeView
        swipeView.setColorSchemeResources(R.color.main);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LogUtil.d("Swipe", "Refreshing number " + number);
                requestIssue();
            }
        });

        // recreate
        if (savedInstanceState != null) {
            LogUtil.d(TAG, "create from savedInstanceState");
            date = savedInstanceState.getString(Constant.KEY_DATE);
            number = savedInstanceState.getInt(Constant.KEY_NUMBER);
            setActionBar();

            // posts
            onPostsRequest(savedInstanceState.<Post>getParcelableArrayList(Constant.KEY_SAVED_POSTS));
            return;
        }

        // get the issue number to request
        number = getArguments().getInt(ISSUE_NUMBER);

        // set title
        if (number == -1) {
            updateTitle(null, -1);
        }
        requestIssue();
    }

    private void requestIssue() {
        if (swipeView == null || !isAdded()) {
            // refreshing
            return;
        }

        // call setRefreshing directly will not trigger the animation
        swipeView.post(new Runnable() {
            @Override
            public void run() {
                swipeView.setRefreshing(true);
            }
        });

        // connected to internet
        if (NetworkUtil.getConnectivityStatus(getActivity()) != NetworkUtil.TYPE_NOT_CONNECTED) {
            // request issue
            String path = Constant.baseUrl + Constant.issueUrl;
            if (number < 0) {
                path += "/latest";
            } else {
                path += "/" + String.valueOf(number);
            }

            LogUtil.d(TAG, "netword connected, request link: " + path);
            Map<String, String> headers = new HashMap<>();
            GsonRequest<IssueResult> requester = new GsonRequest<>(path, IssueResult.class,
                    headers, this, this);
            postQueue.add(requester);
        } else {
            // get from local storage
            LogUtil.d(TAG, "network not connected, request from local db");
            if (number < 0) {
                Iterator<PostModel> biggestId = PostModel.findAsIterator(PostModel.class, null, null, null, "number desc", null);
                if (!biggestId.hasNext()) {
                    LogUtil.d(TAG, "no post");
                    Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
                    swipeView.post(new Runnable() {
                        @Override
                        public void run() {
                            swipeView.setRefreshing(false);
                        }
                    });
                    return;
                } else {
                    number = biggestId.next().number;
                    LogUtil.d(TAG, "the biggest postId is " + number);
                }
            }

            List<PostModel> postModels = PostModel.find(PostModel.class, "number = ?", String.valueOf(number));
            if (postModels.isEmpty()) {
                LogUtil.d(TAG, "no such post [number: " + number + "]");
                Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
                swipeView.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                    }
                });
            } else {
                // update title
                date = postModels.get(0).date;
                number = postModels.get(0).number;
                setActionBar();

                // update posts
                List<Post> posts = new ArrayList<>();
                for (PostModel model: postModels) {
                    posts.add(model.getPost());
                }
                onPostsRequest(posts);
            }
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        swipeView.setRefreshing(false);
        if (isAdded()) {
            LogUtil.e(TAG, "error when request issue: " + error.getMessage());
            Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResponse(IssueResult response) {
        swipeView.setRefreshing(false);
        if (response == null || response.code == 2 || response.data == null || response.data.posts.isEmpty()) {
            LogUtil.e(TAG, "no posts got");
            if (isAdded()) {
                Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        date = response.data.date;
        number = response.data.number;
        LogUtil.d(TAG, "get response data => date: " + date + ", number: " + number);

        setActionBar();
        onPostsRequest(response.data.posts);
        savePosts(response.data);
    }

    /**
     * Set title and global share content
     */
    private void setActionBar() {
        // update title
        updateTitle(date, number);

        // set share intent
        String body = getShareBody(date, number);
        if (shareListener == null) {
            LogUtil.e(TAG, "No OnShareListener is set");
            return;
        }

        if (body == null) {
            shareListener.onRestoreGlobalShare();
        } else {
            shareListener.onGlobalShareContentChanged(getString(R.string.share_post),
                    getShareBody(date, number));
        }
    }

    /**
     * Get share content for current issue
     * @param date      Issue date
     * @param number    Issue number
     * @return  Share body of the issue
     */
    private String getShareBody(String date, int number) {
        if (date == null || number < 0) {
            return null;
        }

        return getString(R.string.title_pattern_long, date, number) +
                getString(R.string.via_app, Constant.playUrl) +
                Constant.wanquRootUrl + Constant.issuesUrl + "/" + number;
    }

    /**
     * Called when new posts are got, either from network or local database
     * @param posts New posts
     */
    private void onPostsRequest(List<Post> posts) {
        if (posts == null) {
            return;
        }

        swipeView.post(new Runnable() {
            @Override
            public void run() {
                swipeView.setRefreshing(false);
            }
        });

        this.posts = posts;
        if (postAdapter != null) {
            postAdapter.notifyDataSetChanged();
        }
        LogUtil.d(TAG, "posts refreshed");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogUtil.d(TAG, "save posts, date: " + date + " number: " + number);

        // save posts
        outState.putParcelableArrayList(Constant.KEY_SAVED_POSTS, (ArrayList<? extends Parcelable>) posts);

        // save date and number of the issue
        outState.putString(Constant.KEY_DATE, date);
        outState.putInt(Constant.KEY_NUMBER, number);
    }

    private void updateTitle(String date, int number) {
        if (!isAdded()) {
            return;
        }

        CharSequence title;
        if (date == null || number < 0) {
            title = getString(R.string.title_section_latest);
        } else {
            title = getString(R.string.title_pattern, date, number);
        }

        LogUtil.d(TAG, "set title to " + title);
        ((MainActivity) getActivity()).updateTitle(title);
    }

    private void savePosts(final PostWrapper issue) {
        if (issue == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < issue.posts.size(); ++i) {
                    Post post = issue.posts.get(i);
                    if (!PostModel.find(PostModel.class, "post_id = ?", String.valueOf(post.id)).isEmpty()) {
                        LogUtil.d(TAG, "post with id: " + post.id + " already exists");
                        continue;
                    }
                    PostModel model = new PostModel(post, issue.date, issue.number);
                    model.save();
                }
            }
        }).run();
    }

    public void setOnShareListner(OnShareListener listener) {
        if (listener == null) {
            return;
        }

        shareListener = listener;
    }

    public void openUrl(String url, String subject, String body) {
        if (url == null || !isAdded()) {
            return;
        }

        LogUtil.d(TAG, "openUrl, url: " + url + " subject: " + subject + " body: " + body);

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
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

    public void openHtml(String html, String subject, String body) {
        if (html == null || !isAdded()) {
            return;
        }

        LogUtil.d(TAG, "openHtml, subject: " + subject + " body: " + body);

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
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

    class PostAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (posts == null) {
                return 0;
            }
            return posts.size();
        }

        @Override
        public Object getItem(int position) {
            if (posts == null) {
                return null;
            }
            return posts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!isAdded()) {
                return null;
            }

            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.list_item_post, null);

                holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
                holder.tvDomain = (TextView) convertView.findViewById(R.id.tv_domain);
                holder.tvSummary = (TextView) convertView.findViewById(R.id.tv_summary);
                holder.btnShare = (Button) convertView.findViewById(R.id.btn_share);
                holder.btnComment = (Button) convertView.findViewById(R.id.btn_comment);
                holder.btnOriginal = (Button) convertView.findViewById(R.id.btn_original);
                holder.btnEasyRead = (Button) convertView.findViewById(R.id.btn_easy_read);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // set value
            final Post post = (Post) getItem(position);

            if (post != null) {
                holder.tvTitle.setText(post.title);
                holder.tvDomain.setText(post.urlDomain);
                holder.tvSummary.setText(post.summary);

                //  share
                holder.btnShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (shareListener == null) {
                            return;
                        }
                        String subject = getString(R.string.share_post);
                        String link = Constant.wanquRootUrl + "/p/" + String.valueOf(post.issueId);
                        String body = String.format(Locale.getDefault(),
                                "【%s】%s %s",
                                getString(R.string.app_name),
                                getString(R.string.via_app, Constant.playUrl),
                                link);
                        shareListener.onShareText(subject, body);
                    }
                });

                // comment
                holder.btnComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openUrl(Constant.wanquRootUrl + "/" + post.slug, null, post.getShareBody(getActivity()));
                    }
                });

                // original
                holder.btnOriginal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openUrl(post.url, null, post.getShareBody(getActivity()));
                    }
                });

                // easy easy
                holder.btnEasyRead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openHtml(post.readableArticle, null, post.getShareBody(getActivity()));
                    }
                });
            }
            return convertView;
        }

        private class ViewHolder {
            TextView tvTitle;
            TextView tvDomain;
            TextView tvSummary;
            Button btnShare;
            Button btnComment;
            Button btnOriginal;
            Button btnEasyRead;
        }
    }
}
