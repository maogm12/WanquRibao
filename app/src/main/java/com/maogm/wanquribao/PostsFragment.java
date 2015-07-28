package com.maogm.wanquribao;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.maogm.wanquribao.Adapter.PostAdapter;
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
import java.util.Map;

/**
 * A fragment containing issue
 * @author Guangming Mao
 */
public class PostsFragment extends Fragment implements Response.Listener<IssueResult>, Response.ErrorListener {

    private static final String TAG = "PostsFragment";

    private static final String ISSUE_NUMBER = "issue_number";
    private static final String POST_TAG = "post_tag";

    /**
     * Detemine which what we are querying
     */
    private enum PostsQueryType {
        ISSUE_NUMBER, POST_TAG
    }
    private PostsQueryType queryType;

    private String date;
    private int number = -1;
    private String tag = null;
    private List<Post> posts;
    private PostAdapter postAdapter;
    private SwipeRefreshLayout swipeView;

    private OnShareListener shareListener;

    // request queue
    private RequestQueue postQueue;

    /**
     * Returns a new instance of this fragment for the given issue number.
     */
    public static PostsFragment newInstance(int number) {
        PostsFragment fragment = new PostsFragment();
        Bundle args = new Bundle();
        args.putInt(ISSUE_NUMBER, number);
        fragment.setArguments(args);
        fragment.queryType = PostsQueryType.ISSUE_NUMBER;
        return fragment;
    }

    /**
     * Returns a new instance of this fragment for the given tag.
     */
    public static PostsFragment newInstance(String tag) {
        PostsFragment fragment = new PostsFragment();
        Bundle args = new Bundle();
        args.putString(POST_TAG, tag);
        fragment.setArguments(args);
        fragment.queryType = PostsQueryType.POST_TAG;
        return fragment;
    }

    public static PostsFragment newInstance() {
        return newInstance(-1);
    }

    public PostsFragment() {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postAdapter = new PostAdapter(getActivity());
        postAdapter.setOnShareListener((MainActivity) getActivity());
        postAdapter.setWebViewManager((MainActivity) getActivity());
        postAdapter.setPostManager((MainActivity) getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // swipeView
        swipeView = (SwipeRefreshLayout) view.findViewById(R.id.swipe);

        swipeView.setColorSchemeResources(R.color.main);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                switch (queryType) {
                    case ISSUE_NUMBER:
                        LogUtil.d("Swipe", "Refreshing number " + number);
                        requestPostsByNumber();
                        break;
                    case POST_TAG:
                        LogUtil.d("Swipe", "Refreshing tag " + number);
                        requestPostsByTag();
                        break;
                }
            }
        });

        // list
        RecyclerView recyclerPost = (RecyclerView) view.findViewById(R.id.recycler_post);
        recyclerPost.setLayoutManager(new LinearLayoutManager(getActivity()));

        // disable swipe view when not in the top
        recyclerPost.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int topRowVerticalPosition = (recyclerView == null || recyclerView.getChildCount() == 0) ?
                        0 :
                        recyclerView.getChildAt(0).getTop();
                swipeView.setEnabled(topRowVerticalPosition >= 0);
            }
        });

        // adapter
        recyclerPost.setAdapter(postAdapter);

        // recreate
        if (savedInstanceState != null) {
            LogUtil.d(TAG, "create from savedInstanceState");
            queryType = (PostsQueryType) savedInstanceState.getSerializable(Constant.KEY_QUERY_TYPE);
            date = savedInstanceState.getString(Constant.KEY_DATE);
            number = savedInstanceState.getInt(Constant.KEY_NUMBER);
            tag = savedInstanceState.getString(Constant.KEY_TAG);
            setActionBar();

            // posts
            onPostsRequest(savedInstanceState.<Post>getParcelableArrayList(Constant.KEY_SAVED_POSTS));
            return;
        }

        // get the issue number/tag to request
        number = getArguments().getInt(ISSUE_NUMBER);
        tag = getArguments().getString(POST_TAG);

        // come back
        if (posts != null) {
            onPostsRequest(posts);
            return;
        }

        updateTitle();
        switch (queryType) {
            case ISSUE_NUMBER:
                requestPostsByNumber();
                break;
            case POST_TAG:
                requestPostsByTag();
                break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        postQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    private void requestPostsByUrl(String url) {
        LogUtil.d(TAG, "netword connected, request link: " + url);
        Map<String, String> headers = new HashMap<>();
        GsonRequest<IssueResult> requester = new GsonRequest<>(url, IssueResult.class,
                headers, this, this);
        postQueue.add(requester);
    }

    private void requestPostsByTag() {
        if (swipeView == null || !isAdded() || tag == null) {
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
            String url = Constant.baseUrl + Constant.tagsUrl + "/" + tag;
            requestPostsByUrl(url);
        } else {
            // get from local storage
            LogUtil.d(TAG, "network not connected, request from local db");

            List<PostModel> postModels = PostModel.find(PostModel.class, "number LIKE ?", tag);
            if (postModels.isEmpty()) {
                LogUtil.d(TAG, "no such post [tag: " + tag + "]");
                Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
                swipeView.setRefreshing(false);
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

    private void requestPostsByNumber() {
        if (swipeView == null || !isAdded()) {
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
        updateTitle();

        // set share intent
        String body = getShareBody();

        if (shareListener == null) {
            LogUtil.e(TAG, "No OnShareListener is set");
            return;
        }

        if (body == null) {
            shareListener.onRestoreGlobalShare();
        } else {
            shareListener.onGlobalShareContentChanged(getString(R.string.share_post), body);
        }
    }

    /**
     * Get share content for current issue
     * @return  Share body of the issue
     */
    private String getShareBody() {
        if (!isAdded()) {
            return null;
        }

        switch (queryType) {
            case ISSUE_NUMBER:
                if (date == null || number < 0) {
                    return null;
                }

                return getString(R.string.title_pattern_long, date, number) +
                        getString(R.string.via_app, Constant.playUrl) +
                        Constant.wanquRootUrl + Constant.issuesUrl + "/" + number;
            case POST_TAG:

                if (tag == null) {
                    return null;
                }

                return getString(R.string.title_tag, tag) +
                        getString(R.string.via_app, Constant.playUrl) +
                        Constant.wanquRootUrl + Constant.tagUrl + "/" + tag;
        }

        return null;
    }

    /**
     * Called when new posts are got, either from network or local database
     * @param posts New posts
     */
    private void onPostsRequest(List<Post> posts) {
        if (posts == null || this.posts == posts) {
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
            postAdapter.setPosts(posts);
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

        // save query type
        outState.putSerializable(Constant.KEY_QUERY_TYPE, queryType);

        switch (queryType) {
            case ISSUE_NUMBER:
                // save date and number of the issue
                outState.putString(Constant.KEY_DATE, date);
                outState.putInt(Constant.KEY_NUMBER, number);
                break;
            case POST_TAG:
                outState.putString(Constant.KEY_TAG, tag);
                break;
            default:
                break;
        }

    }

    private void updateTitle() {
        if (!isAdded()) {
            return;
        }

        CharSequence title = null;
        switch (queryType) {
            case ISSUE_NUMBER:
                if (date == null || number < 0) {
                    title = getString(R.string.title_section_latest);
                } else {
                    title = getString(R.string.title_pattern, date, number);
                }

                LogUtil.d(TAG, "set title to " + title);
                break;
            case POST_TAG:
                if (tag == null) {
                    title = getString(R.string.app_name);
                } else {
                    title = getString(R.string.title_tag, tag);
                }

                LogUtil.d(TAG, "set title to tag: " + title);
        }

        if (title != null) {
            ((MainActivity) getActivity()).updateTitle(title);
        }
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
}
