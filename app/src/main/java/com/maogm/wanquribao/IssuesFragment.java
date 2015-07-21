package com.maogm.wanquribao;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.maogm.wanquribao.Listener.OnShareListener;
import com.maogm.wanquribao.Module.Issue;
import com.maogm.wanquribao.Module.IssuesResult;
import com.maogm.wanquribao.Utils.Constant;
import com.maogm.wanquribao.Utils.LogUtil;
import com.maogm.wanquribao.Utils.NetworkUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link IssuesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IssuesFragment extends Fragment implements Response.Listener<IssuesResult>, Response.ErrorListener{

    private static final String TAG = "IssuesFragment";

    private List<Issue> issues;
    private ListView listIssue;
    private IssueAdapter issueAdapter;
    private SwipeRefreshLayout swipeView;

    private OnIssueSelectedListener issueSelectedListener;
    private OnShareListener shareListener;

    // request queue
    private RequestQueue issueQueue;

    public static IssuesFragment newInstance() {
        return new IssuesFragment();
    }

    public IssuesFragment() {
        LogUtil.d(TAG, "new IssueFrament");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnShareListener) {
            shareListener = (OnShareListener) activity;
            shareListener.onGlobalShareEnabled(false);
        } else {
            throw new IllegalArgumentException("activity must implement OnShareListener");
        }

        if (activity instanceof OnIssueSelectedListener) {
            issueSelectedListener = (OnIssueSelectedListener) activity;
        } else {
            throw new IllegalArgumentException("ativity must implement OnIssueSelectedListener");
        }

        issueQueue = Volley.newRequestQueue(activity);

        LogUtil.d(TAG, "onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_issue_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // list
        listIssue = (ListView) view.findViewById(R.id.issue_list);
        issueAdapter = new IssueAdapter();
        listIssue.setAdapter(issueAdapter);
        listIssue.setOnScrollListener(new AbsListView.OnScrollListener() {
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

                int position = listIssue.getFirstVisiblePosition();
                View v = listIssue.getChildAt(position);
                int offset = (v == null) ? 0 : v.getTop();

                // disable swipe view when scrolled down
                if (mPosition < position || mPosition == position && mOffset < offset) {
                    swipeView.setEnabled(false);
                } else {
                    swipeView.setEnabled(true);
                }
            }
        });

        // open issue
        listIssue.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogUtil.d(TAG, "clicked issue at position: " + String.valueOf(position));
                if (issueSelectedListener != null) {
                    Issue issue = issues.get(position);
                    issueSelectedListener.openIssue(issue.number);
                }
            }
        });

        // swipeView
        swipeView = (SwipeRefreshLayout) view.findViewById(R.id.swipe);
        swipeView.setColorSchemeResources(R.color.main);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LogUtil.d("Swipe", "Refresh past issues");
                requestIssues();
            }
        });

        // set title
        if (isAdded()) {
            ((MainActivity) getActivity()).updateTitle(getString(R.string.title_section_past));
        }

        if (savedInstanceState != null) {
            onIssuesRequested(savedInstanceState.<Issue>getParcelableArrayList(Constant.KEY_ISSUES));
        }

        requestIssues();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shareListener != null) {
            shareListener.onGlobalShareEnabled(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        LogUtil.d(TAG, "issues saved to state");
        outState.putParcelableArrayList(Constant.KEY_ISSUES, (ArrayList<? extends Parcelable>) issues);
    }

    public void setOnIssueSelectedListener(OnIssueSelectedListener listener) {
        issueSelectedListener = listener;
    }

    private void requestIssues() {
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
            String path = Constant.baseUrl + Constant.issuesUrl;
            LogUtil.d(TAG, "network connected, request link: " + path);

            Map<String, String> headers = new HashMap<>();
            GsonRequest<IssuesResult> requester = new GsonRequest<>(path, IssuesResult.class,
                    headers, this, this);
            issueQueue.add(requester);
        } else {
            // get from local storage
            LogUtil.d(TAG, "network not connected, request from local db");
            List<Issue> issues = Issue.find(Issue.class, null, null, null, "number desc", null);
            onIssuesRequested(issues);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        swipeView.setRefreshing(false);
        LogUtil.e(TAG, "request error: " + error.getMessage());
        if (getActivity() != null) {
            Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResponse(IssuesResult response) {
        swipeView.setRefreshing(false);
        if (response.code == 2 || response.data == null || response.data.issues.isEmpty()) {
            LogUtil.e(TAG, "no issues got");
            if (isAdded()) {
                Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        onIssuesRequested(response.data.issues);
    }

    private void onIssuesRequested(final List<Issue> issues) {
        if (issues == null) {
            return;
        }

        // stop the animation
        swipeView.post(new Runnable() {
            @Override
            public void run() {
                swipeView.setRefreshing(false);
            }
        });

        if (issues.isEmpty() && isAdded()) {
            Toast.makeText(getActivity(), R.string.no_issue, Toast.LENGTH_SHORT).show();
        }

        this.issues = issues;
        if (issueAdapter != null) {
            issueAdapter.notifyDataSetChanged();
        }

        // save issues
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < issues.size(); ++i) {
                    Issue issue = issues.get(i);
                    if (!Issue.find(Issue.class, "number = ?", String.valueOf(issue.number)).isEmpty()) {
                        LogUtil.d(TAG, "issue with number: " + issue.number + " already exist");
                        continue;
                    }
                    issue.save();
                }
                LogUtil.d(TAG, "issued saved");
            }
        }).run();
    }

    class IssueAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return issues == null ? 0 : issues.size();
        }

        @Override
        public Object getItem(int position) {
            return issues == null ? 0 : issues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.list_item_issue, null);

                holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
                holder.tvSummary = (TextView) convertView.findViewById(R.id.tv_summary);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // value
            Issue issue = (Issue) getItem(position);
            if (issue != null) {
                holder.tvTitle.setText(getString(R.string.title_pattern, issue.date, issue.number));
                holder.tvSummary.setText(issue.summary);
            }
            return convertView;
        }

        class ViewHolder {
            TextView tvTitle;
            TextView tvSummary;
        }
    }

    /**
     * OnIssueSelectedListener
     * @author Guangming Mao
     */
    public static interface OnIssueSelectedListener {
        void openIssue(int number);
    }
}
