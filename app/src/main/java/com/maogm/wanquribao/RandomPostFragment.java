package com.maogm.wanquribao;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.maogm.wanquribao.Module.IssueResult;
import com.maogm.wanquribao.Module.IssueWrapper;
import com.maogm.wanquribao.Module.Post;
import com.maogm.wanquribao.Module.PostModel;
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
                    openUrl(Constant.wanquRootUrl + "/" + fetchedPost.slug);
                }
            }
        });

        // original
        btnOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null) {
                    openUrl(fetchedPost.url);
                }
            }
        });

        // easy easy
        btnEasyRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fetchedPost != null) {
                    openHtml(fetchedPost.readableArticle);
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
        if (response.code == 2) {
            Toast.makeText(getActivity(), R.string.issue_number_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (response.data == null) {
            Log.e(TAG, "no data received");
            return;
        }

        updateTitle(response.data);
        savePosts(response.data);
        if (response.data.posts.isEmpty()) {
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
        } else {
            onPostFetched(response.data.posts.get(0));
        }
    }

    private void updateTitle(IssueWrapper data) {
        if (data == null) {
            return;
        }

        String pattern = getString(R.string.title_pattern);
        ((MainActivity)getActivity()).updateTitle(String.format(Locale.getDefault(), pattern, data.date, data.number));
    }

    private void savePosts(IssueWrapper data) {
        if (data == null) {
            return;
        }

        for (int i = 0; i < data.posts.size(); ++i) {
            Post post = data.posts.get(i);
            if (!PostModel.find(PostModel.class, "id = ?", String.valueOf(post.id)).isEmpty()) {
                continue;
            }
            PostModel model = new PostModel(post, data.date, data.number);
            model.save();
        }
    }

    public void onPostFetched(Post post) {
        if (post == null) {
            return;
        }

        tvTitle.setText(post.title);
        tvUrlDomain.setText(post.urlDomain);
        tvSummary.setText(post.summary);
    }

    /**
     * set a OnShareLinstener for the fragment
     * @param listener the OnShareListener
     */
    public void setOnShareListener(OnShareListener listener) {
        if (listener == null) {
            return;
        }

        shareListener = listener;
    }

    public void openUrl(String url) {
        if (url == null) {
            return;
        }

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_URL, url);
        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }

    public void openHtml(String html) {
        if (html == null) {
            return;
        }

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_HTML, html);
        webViewIntent.putExtras(bundle);
        startActivity(webViewIntent);
    }
}
