package com.maogm.wanquribao.Adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Listener.WebViewManager;
import com.maogm.wanquribao.Module.Post;
import com.maogm.wanquribao.R;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;

import java.util.List;
import java.util.Locale;

/**
 * PostAdapter for RecyclerView
 *
 * @author Guangming Mao
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";

    Context context;
    private List<Post> posts;
    private OnShareListener shareListener;
    private WebViewManager webViewManager;

    public PostAdapter(Context context) {
        this.context = context;
        this.posts = null;
        this.shareListener = null;
    }

    public void setOnShareListener(OnShareListener shareListener) {
        this.shareListener = shareListener;
    }

    public void setWebViewManager(WebViewManager manager) {
        webViewManager = manager;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View cardView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_post, parent, false);
        return new PostViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        if (posts == null) {
            return;
        }

        final Post post = posts.get(position);
        holder.tvTitle.setText(post.title);
        holder.tvDomain.setText(post.urlDomain);
        holder.tvSummary.setText(post.summary);

        holder.btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareListener == null || context == null) {
                    return;
                }

                String subject = context.getString(R.string.share_post);
                String link = Constant.wanquRootUrl + "/p/" + String.valueOf(post.issueId);
                String body = String.format(Locale.getDefault(),
                        "【%s】%s %s",
                        context.getString(R.string.app_name),
                        context.getString(R.string.via_app, Constant.playUrl),
                        link);
                shareListener.onShareText(subject, body);
            }
        });

        // comment
        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webViewManager == null) {
                    LogUtil.e(TAG, "No WebViewmanager");
                    return;
                }

                webViewManager.openUrl(
                        Constant.wanquRootUrl + "/" + post.slug,
                        null,
                        post.getShareBody(context));
            }
        });

        // original
        holder.btnOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webViewManager == null) {
                    LogUtil.e(TAG, "No WebViewmanager");
                    return;
                }

                webViewManager.openUrl(
                        post.url,
                        null,
                        post.getShareBody(context));
            }
        });

        // easy easy
        holder.btnEasyRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webViewManager == null) {
                    LogUtil.e(TAG, "No WebViewmanager");
                    return;
                }

                webViewManager.openHtml(
                        post.readableArticle,
                        null,
                        post.getShareBody(context));
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }


    public static class PostViewHolder extends RecyclerView.ViewHolder {
        CardView cardview;
        TextView tvTitle;
        TextView tvDomain;
        TextView tvSummary;
        ImageButton btnShare;
        ImageButton btnComment;
        ImageButton btnOriginal;
        ImageButton btnEasyRead;

        PostViewHolder(View itemView) {
            super(itemView);

            cardview = (CardView) itemView.findViewById(R.id.card_post);
            tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            tvDomain = (TextView) itemView.findViewById(R.id.tv_domain);
            tvSummary = (TextView) itemView.findViewById(R.id.tv_summary);
            btnShare = (ImageButton) itemView.findViewById(R.id.btn_share);
            btnComment = (ImageButton) itemView.findViewById(R.id.btn_comment);
            btnOriginal = (ImageButton) itemView.findViewById(R.id.btn_original);
            btnEasyRead = (ImageButton) itemView.findViewById(R.id.btn_easy_read);
        }
    }
}