package com.maogm.wanquribao;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Module.IssueResult;
import com.maogm.wanquribao.Module.PostWrapper;
import com.maogm.wanquribao.Module.Post;
import com.maogm.wanquribao.Module.PostModel;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.NetworkUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class RandomPostFragment extends Fragment implements Response.Listener<IssueResult>, Response.ErrorListener {

    private static final String TAG = "RandomPostFragment";

    private Post fetchedPost = null;

    private TextView tvTitle;
    private TextView tvUrlDomain;
    private TextView tvSummary;
    private Button btnShare;
    private Button btnComment;
    private Button btnOriginal;
    private Button btnEasyRead;

    private Button btnRandom;

    // OnShareLinstener, used to share stuff
    private OnShareListener shareListener;

    /**
     * Use this factory method to create a new instance of this fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RandomPostFragment newInstance() {
        return new RandomPostFragment();
    }

    public RandomPostFragment() {
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
        btnShare = (Button) view.findViewById(R.id.btn_share);
        btnComment = (Button) view.findViewById(R.id.btn_comment);
        btnOriginal = (Button) view.findViewById(R.id.btn_original);
        btnEasyRead = (Button) view.findViewById(R.id.btn_easy_read);
        btnRandom = (Button) view.findViewById(R.id.btn_random);

        // share
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null) {
                    String subject = getString(R.string.share_post);
                    String link = Constant.wanquRootUrl + "/p/" + String.valueOf(fetchedPost.issueId);
                    StringBuffer sb = new StringBuffer();
                    sb.append("【").append(fetchedPost.title).append("】")
                            .append("(via ").append(getString(R.string.app_name))
                            .append(": ").append(Constant.playUrl)
                            .append(") ").append(link);
                    String body = sb.toString();
                    shareListener.shareText(subject, body);
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
        btnEasyRead.setOnClickListener(new View.OnClickListener() {
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

        // fetched a random post
        fetchRandomPost();
    }

    private void fetchRandomPost() {
        if (getActivity() == null) {
            return;
        }

        Activity activity = getActivity();

        // no network, fetch from local storage
        if (NetworkUtil.getConnectivityStatus(activity) == NetworkUtil.TYPE_NOT_CONNECTED) {
            long saveSize = PostModel.count(PostModel.class, null, null);
            // NO local saved post
            if (saveSize == 0) {
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
                onPostFetched(all.next().getPost());
            }
        } else {
            String path = Constant.baseUrl + Constant.randomPostUrl;
            Map<String, String> headers = new HashMap<>();
            GsonRequest<IssueResult> requester = new GsonRequest<>(path, IssueResult.class,
                    headers, this, this);
            ((MainActivity) activity).AddRequest(requester);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, error.getMessage());
        Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(IssueResult response) {
        if (response == null || response.code == 2 || response.data == null) {
            Log.d(TAG, "no post got");
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
            return;
        }

        updateTitle(response.data);
        savePosts(response.data);
        if (response.data.posts.isEmpty()) {
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
        } else {
            onPostFetched(response.data.posts.get(0));
        }

        // set intern
        ((MainActivity)getActivity()).setShareIntent(getShareIntent(response.data));
    }

    private void updateTitle(PostWrapper data) {
        if (data == null) {
            return;
        }

        String pattern = getString(R.string.title_pattern);
        ((MainActivity)getActivity()).updateTitle(String.format(Locale.getDefault(), pattern, data.date, data.number));
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

    private Intent getShareIntent(PostWrapper data) {
        if (data == null) {
            return null;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_me));
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.title_pattern_long, data.date, data.number))
                .append(getString(R.string.via_app, Constant.playUrl))
                .append(Constant.wanquRootUrl).append(Constant.issuesUrl)
                .append("/").append(data.number);
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        intent.setType("text/plain");
        return intent;
    }

    /**
     * set a OnShareLinstener for the fragment
     * @param listener the OnShareListener
     */
    public void setOnShareListener(OnShareListener listener) {
        shareListener = listener;
    }

    public void openUrl(String url, String subject, String body) {
        if (url == null) {
            return;
        }

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_URL, url);

        if (subject == null) {
            subject = getString(R.string.share_link);
        }
        bundle.putString(Constant.KEY_SHARE_SUBJECT, subject);
        if (body != null) {
            bundle.putString(Constant.KEY_SHARE_BODY, body + getString(R.string.via_app, Constant.playUrl) + url);
        }

        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }

    public void openHtml(String html, String subject, String body) {
        if (html == null) {
            return;
        }

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
