package com.maogm.wanquribao;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class RandomPostFragment extends Fragment implements Response.Listener<IssueResult>, Response.ErrorListener {

    private static final String TAG = "RandomPostFragment";

    private Post fetchedPost = null;

    private TextView tvTitle;
    private TextView tvUrlDomain;
    private TextView tvSummary;
    private ImageButton btnShare;
    private ImageButton btnComment;
    private ImageButton btnOriginal;
    private ImageButton btnTags;

    private Button btnRandom;

    // OnShareLinstener, used to share stuff
    private OnShareListener shareListener;

    // request queue
    private RequestQueue postQueue;

    private String date;
    private int number;

    /**
     * Use this factory method to create a new instance of this fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RandomPostFragment newInstance() {
        return new RandomPostFragment();
    }

    public RandomPostFragment() {
        LogUtil.d(TAG, "new RandomPostFragment");
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_random_post, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvUrlDomain = (TextView) view.findViewById(R.id.tv_domain);
        tvSummary = (TextView) view.findViewById(R.id.tv_summary);
        btnShare = (ImageButton) view.findViewById(R.id.btn_share);
        btnComment = (ImageButton) view.findViewById(R.id.btn_comment);
        btnOriginal = (ImageButton) view.findViewById(R.id.btn_original);
        btnTags = (ImageButton) view.findViewById(R.id.btn_tags);
        btnRandom = (Button) view.findViewById(R.id.btn_random);

        if (savedInstanceState != null) {
            fetchedPost = savedInstanceState.getParcelable(Constant.KEY_RANDOM_POST);
            date = savedInstanceState.getString(Constant.KEY_DATE);
            number = savedInstanceState.getInt(Constant.KEY_NUMBER);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // share
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null && isAdded()) {
                    String subject = getString(R.string.share_post);
                    String link = Constant.wanquRootUrl + "/p/" + String.valueOf(fetchedPost.issueId);
                    String body = "【" + fetchedPost.title + "】" + getString(R.string.via_app, Constant.playUrl) + link;
                    shareListener.onShareText(subject, body);
                }
            }
        });

        // comment
        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null) {
                    openUrl(Constant.wanquRootUrl + "/" + fetchedPost.slug, null, fetchedPost.getShareBody(getActivity()));
                }
            }
        });

        // original
        btnOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null) {
                    openUrl(fetchedPost.url, null, fetchedPost.getShareBody(getActivity()));
                }
            }
        });

        // easy easy
        btnTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null) {
                    openHtml(fetchedPost.readableArticle, null, fetchedPost.getShareBody(getActivity()));
                }
            }
        });

        // random
        btnRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchRandomPost();
            }
        });

        // set title
        if (isAdded()) {
            ((MainActivity) getActivity()).updateTitle(getString(R.string.title_section_random));
        }

        if (fetchedPost == null) {
            // fetched a random post
            fetchRandomPost();
        } else {
            onPostFetched(fetchedPost);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        LogUtil.d(TAG, "save instance state");

        outState.putParcelable(Constant.KEY_RANDOM_POST, fetchedPost);
        outState.putString(Constant.KEY_DATE, date);
        outState.putLong(Constant.KEY_NUMBER, number);
    }

    private void fetchRandomPost() {
        if (!isAdded()) {
            return;
        }

        Activity activity = getActivity();

        // no network, fetch from local storage
        if (NetworkUtil.getConnectivityStatus(activity) == NetworkUtil.TYPE_NOT_CONNECTED) {
            long saveSize = PostModel.count(PostModel.class, null, null);
            // NO local saved post
            if (saveSize == 0) {
                LogUtil.d(TAG, "no issue");
                Toast.makeText(activity, R.string.network_error, Toast.LENGTH_SHORT).show();
                return;
            }

            Random random = new Random();
            long index = random.nextLong() % saveSize;

            // if any post saved, random one
            Iterator<PostModel> all = PostModel.findAll(PostModel.class);
            while (index-- > 0 && all.hasNext()) {
                all.next();
            }

            if (all.hasNext()) {
                LogUtil.d(TAG, "parse post");

                PostModel model = all.next();
                date = model.date;
                number = model.number;
                setActionBar();

                onPostFetched(model.getPost());
            } else {
                LogUtil.d(TAG, "no issue");
                Toast.makeText(activity, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            String path = Constant.baseUrl + Constant.randomPostUrl;
            LogUtil.d(TAG, "necword connected, quest pat: " + path);
            Map<String, String> headers = new HashMap<>();
            GsonRequest<IssueResult> requester = new GsonRequest<>(path, IssueResult.class,
                    headers, this, this);
            postQueue.add(requester);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        LogUtil.e(TAG, "request error: " + error.getMessage());
        Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(IssueResult response) {
        if (response == null || response.code == 2 || response.data == null || !isAdded()) {
            LogUtil.d(TAG, "no post got");
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
            return;
        }

        // set date and number
        date = response.data.date;
        number = response.data.number;
        setActionBar();

        savePosts(response.data);
        if (response.data.posts.isEmpty()) {
            LogUtil.d(TAG, "no post got");
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
        } else {
            onPostFetched(response.data.posts.get(0));
        }
    }

    private void setActionBar() {
        if (!isAdded()) {
            return;
        }

        updateTitle(date, number);
        if (shareListener != null) {
            shareListener.onGlobalShareContentChanged(getString(R.string.share_post),
                    getShareBody(date, number));
        }
    }

    private void updateTitle(String date, int number) {
        if (!isAdded()) {
            return;
        }

        String title;
        if (date == null) {
            title = getString(R.string.title_section_random);
        } else {
            title = getString(R.string.title_pattern, date, number);
        }

        ((MainActivity)getActivity()).updateTitle(title);
    }

    private String getShareBody(String date, int number) {
        if (date == null || !isAdded()) {
            return null;
        }

        return getString(R.string.title_pattern_long, date, number) +
                getString(R.string.via_app, Constant.playUrl) +
                Constant.wanquRootUrl + Constant.issuesUrl + "/" + number;
    }

    private void savePosts(final PostWrapper data) {
        if (data == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < data.posts.size(); ++i) {
                    Post post = data.posts.get(i);
                    if (!PostModel.find(PostModel.class, "post_id = ?", String.valueOf(post.id)).isEmpty()) {
                        continue;
                    }
                    PostModel model = new PostModel(post, data.date, data.number);
                    model.save();
                }
            }
        }).run();
    }

    public void onPostFetched(Post post) {
        if (post == null) {
            return;
        }

        fetchedPost = post;
        tvTitle.setText(post.title);
        tvUrlDomain.setText(post.urlDomain);
        tvSummary.setText(post.summary);
    }

    /**
     * set a OnShareLinstener for the fragment
     * @param listener the OnShareListener
     */
    public void setOnShareListener(OnShareListener listener) {
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
        if (html == null) {
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
}
