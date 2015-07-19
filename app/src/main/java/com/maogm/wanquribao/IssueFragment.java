package com.maogm.wanquribao;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Module.IssueResult;
import com.maogm.wanquribao.Module.Post;
import com.maogm.wanquribao.Module.PostModel;
import com.maogm.wanquribao.Module.PostWrapper;
import com.maogm.wanquribao.Utils.Constant;
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

    private int number = -1;
    private List<Post> posts;
    private ListView listPost;
    private PostAdapter postAdapter;
    private SwipeRefreshLayout swipeView;

    private OnShareListener shareListener;

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
        swipeView = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        swipeView.setColorSchemeResources(R.color.main);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("Swipe", "Refreshing number " + number);
                requestIssue();
            }
        });

        // get the issue number to request
        number = getArguments().getInt(ISSUE_NUMBER);
        requestIssue();

        // set title
        if (number == -1) {
            ((MainActivity) getActivity()).updateTitle(getString(R.string.title_section_latest));
        }
    }

    private void requestIssue() {
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

            Log.d(TAG, "netword connected, request link: " + path);
            Map<String, String> headers = new HashMap<>();
            GsonRequest<IssueResult> requester = new GsonRequest<>(path, IssueResult.class,
                    headers, this, this);
            ((MainActivity) getActivity()).AddRequest(requester);
        } else {
            // get from local storage
            Log.d(TAG, "network not connected, request from local db");
            if (number < 0) {
                Iterator<PostModel> biggestId = PostModel.findAsIterator(PostModel.class, null, null, null, "number desc");
                if (!biggestId.hasNext()) {
                    Log.d(TAG, "no post");
                    Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    number = biggestId.next().number;
                    Log.d(TAG, "the biggest id is " + number);
                }
            }

            List<PostModel> postModels = PostModel.find(PostModel.class, "number = ?", String.valueOf(number));
            if (postModels.isEmpty()) {
                Log.d(TAG, "no such post [number: " + number + "]");
                Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
            } else {
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
        Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(IssueResult response) {
        swipeView.setRefreshing(false);
        if (response == null || response.code == 2 || response.data == null || response.data.posts.isEmpty()) {
            Log.e(TAG, "no posts got");
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
            return;
        }

        onPostsRequest(response.data.posts);
        updateTitle(response);
        savePosts(response);
        ((MainActivity)getActivity()).setShareIntent(getShareIntent(response.data));
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

    private void onPostsRequest(List<Post> posts) {
        if (posts == null) {
            return;
        }

        this.posts = posts;
        if (postAdapter != null) {
            postAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "posts refreshed");
    }

    private void updateTitle(IssueResult response) {
        if (response == null || response.data == null) {
            return;
        }

        PostWrapper wrapper = response.data;
        String pattern = getString(R.string.title_pattern);
        ((MainActivity)getActivity()).updateTitle(String.format(Locale.getDefault(), pattern, wrapper.date, wrapper.number));
    }

    private void savePosts(IssueResult response) {
        if (response == null) {
            return;
        }
        for (int i = 0; i < response.data.posts.size(); ++i) {
            Post post = response.data.posts.get(i);
            if (!PostModel.find(PostModel.class, "id = ?", String.valueOf(post.id)).isEmpty()) {
                continue;
            }
            PostModel model = new PostModel(post, response.data.date, response.data.number);
            model.save();
        }
    }

    public void setOnShareListner(OnShareListener listener) {
        if (listener == null) {
            return;
        }

        shareListener = listener;
    }

    public void openUrl(String url, String title) {
        if (url == null) {
            return;
        }

        Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.KEY_URL, url);
        bundle.putString(Constant.KEY_SHARE_SUBJECT, getString(R.string.share_post));
        if (title != null) {
            bundle.putString(Constant.KEY_SHARE_BODY, title);
        }
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
            ViewHolder holder = null;
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
                        StringBuffer sb = new StringBuffer();
                        sb.append("【").append(post.title).append("】")
                                .append("(via ").append(getString(R.string.app_name))
                                .append(": ").append(Constant.playUrl)
                                .append(") ").append(link);
                        String body = sb.toString();
                        shareListener.shareText(subject, body);
                    }
                });

                // comment
                holder.btnComment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openUrl(Constant.wanquRootUrl + "/" + post.slug, post.readableTitle);
                    }
                });

                // original
                holder.btnOriginal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openUrl(post.url, post.readableTitle);
                    }
                });

                // easy easy
                holder.btnEasyRead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openHtml(post.readableArticle);
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
