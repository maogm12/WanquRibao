package com.maogm.wanquribao;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment containing issue
 * @author Guangming Mao
 */
public class IssueFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String TAG = "IssueFragment";

    private static final String ISSUE_NUMBER = "issue_number";

    private int number = -1;

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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
