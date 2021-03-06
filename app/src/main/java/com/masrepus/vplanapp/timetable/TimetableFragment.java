package com.masrepus.vplanapp.timetable;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ListView;

import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.vplan.MainActivity;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetableFragment extends Fragment {

    private int lastFirstVisible;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.view_card_timetable, null);
        Bundle args = getArguments();

        //get the listview adapter from args and attach it to the listview
        //args could be corrupted if ram was cleared while in background => restart the activity
        TimetableListAdapter adapter;
        try {
            adapter = (TimetableListAdapter) args.getSerializable(Args.ADAPTER);
        } catch (Exception e) {

            //restart, something is wrong here
            getActivity().finish();
            startActivity(new Intent(getActivity(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
            return View.inflate(getActivity(), R.layout.view_card_loading, null);
        }

        ListView timetable = (ListView) rootView.findViewById(R.id.timetable);
        timetable.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem > lastFirstVisible) {
                    //scrolling down, hide fab
                    FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
                    Animation out = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_out);

                    //only restart if the view is not hidden already
                    if (fab.getVisibility() == View.VISIBLE) {
                        fab.startAnimation(out);
                        fab.setVisibility(View.INVISIBLE);
                    }
                } else if (firstVisibleItem < lastFirstVisible) {
                    //scrolling up, show fab
                    //only restart if the view is still invisible
                    Animation in = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
                    FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);

                    if (fab.getVisibility() == View.INVISIBLE) {
                        fab.startAnimation(in);
                        fab.setVisibility(View.VISIBLE);
                    }
                }
                lastFirstVisible = firstVisibleItem;
            }
        });
        timetable.setAdapter(adapter);

        return rootView;
    }
}
