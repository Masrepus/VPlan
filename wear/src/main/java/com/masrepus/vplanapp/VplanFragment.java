package com.masrepus.vplanapp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.SharedPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by samuel on 16.01.15.
 */
public class VplanFragment extends Fragment {

    private int position;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //get the current position and save it
        Bundle arguments = getArguments();
        position = arguments.getInt(Args.ARG_POSITION);

        View rootView = inflater.inflate(R.layout.vplan_fragment, null);

        SharedPreferences pref = getActivity().getSharedPreferences(SharedPrefs.PREFS_NAME, 0);

        //display vplan data in recylerview
        WearableListView listView = (WearableListView) rootView.findViewById(R.id.vplanlist);

        //make the listview scrollable inside pager
        listView.setGreedyTouchMode(true);

        ArrayList<Row> vplanlist = (ArrayList<Row>) arguments.getSerializable(Args.VPLAN_LIST);
        VplanListAdapter adapter = new VplanListAdapter(getActivity(), vplanlist);

        listView.setAdapter(adapter);

        TextView header = (TextView) rootView.findViewById(R.id.header);

        header.setText(pref.getString(SharedPrefs.PREF_HEADER_PREFIX + position, ""));

        //if the list is empty notify the user that there are no items for today
        if (adapter.getItemCount() == 0) {

            TextView noItemsTV = (TextView) rootView.findViewById(R.id.noItemsTV);
            noItemsTV.setText(getString(R.string.no_data_today));

            FrameLayout noItemsBg = (FrameLayout) rootView.findViewById(R.id.noItemsBg);
            noItemsBg.setVisibility(View.VISIBLE);
        }

        displayTimestamps(pref, rootView);

        rootView.requestApplyInsets();

        return rootView;
    }

    private void displayTimestamps(SharedPreferences pref, View rootView) {

        String timePublished = pref.getString(SharedPrefs.TIME_PUBLISHED_PREFIX + String.valueOf(position), "");

        //display the timestamps in the textviews
        TextView timePublishedTV = (TextView) rootView.findViewById(R.id.timePublishedTV);
        timePublishedTV.setText(getString(R.string.data_update) + timePublished);
    }
}
