package com.masrepus.vplanapp.timetable;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.Args;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetableFragment extends Fragment {

    private int lastFirstVisible;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.timetable_fragment, null);
        Bundle args = getArguments();

        //get the listview adapter from args and attach it to the listview
        TimetableListAdapter adapter = (TimetableListAdapter) args.getSerializable(Args.ADAPTER);
        ListView timetable = (ListView) rootView.findViewById(R.id.timetable);
        timetable.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem > lastFirstVisible) {
                    //scrolling down, hide fab
                    ImageButton fab = (ImageButton) getActivity().findViewById(R.id.fab_image_button);
                    Animation out = AnimationUtils.makeOutAnimation(getActivity(), true);

                    Animation current = fab.getAnimation();
                    //don't restart the animation if it is already running
                    if (current == null || !current.hasStarted() || current.hasEnded()) {
                        //only restart if the view is not hidden already
                        if (fab.getVisibility() == View.VISIBLE) fab.startAnimation(out);
                    }
                    fab.setVisibility(View.INVISIBLE);
                } else if (firstVisibleItem < lastFirstVisible) {
                    //scrolling up, show fab
                    ImageButton fab = (ImageButton) getActivity().findViewById(R.id.fab_image_button);
                    fab.setVisibility(View.VISIBLE);
                }
                lastFirstVisible = firstVisibleItem;
            }
        });
        timetable.setAdapter(adapter);

        return rootView;
    }
}
