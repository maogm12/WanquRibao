package com.maogm.wanquribao;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.maogm.wanquribao.Module.IssueResult;
import com.maogm.wanquribao.Module.Post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment containing issue
 * @author Guangming Mao
 */
public class IssueFragment extends Fragment implements Response.Listener<IssueResult>, Response.ErrorListener {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String TAG = "IssueFragment";

    private static final String ISSUE_NUMBER = "issue_number";

    private int number = -1;
    private List<Post> posts;

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
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.number = getArguments().getInt(ISSUE_NUMBER);

        String path = Constant.baseUri + Constant.latestUri;
        Map<String, String> headers = new HashMap<>();
        GsonRequest<IssueResult> requester = new GsonRequest<>(path, IssueResult.class,
                headers, this, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }

    @Override
    public void onResponse(IssueResult response) {
        if (response.code == 2) {
            Toast.makeText(getActivity(), R.string.issue_number_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        this.posts = response.data.posts;
    }
}
