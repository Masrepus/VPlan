package com.masrepus.vplanapp.timetable;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.Args;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetableFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.timetable_fragment, null);
        Bundle args = getArguments();

        //get the listview adapter from args and attach it to the listview
        TimetableListAdapter adapter = (TimetableListAdapter) args.getSerializable(Args.ADAPTER);
        ListView timetable = (ListView) rootView.findViewById(R.id.timetable);
        timetable.setAdapter(adapter);

        return rootView;
    }
}
