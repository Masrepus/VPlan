package com.masrepus.vplanapp;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by samuel on 18.01.15.
 */
public class EmptyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.empty_fragment, null);

        TextView noItemsTV = (TextView) rootView.findViewById(R.id.noItemsTV);
        noItemsTV.setText(getString(R.string.no_data));

        //if refresh imageview is clicked, then send a request to phone that a bg download should be started
        ImageView refresh = (ImageView) rootView.findViewById(R.id.refresh);
        refresh.setOnClickListener((MainActivity) getActivity());


        return rootView;
    }
}
