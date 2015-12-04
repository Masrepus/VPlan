package com.masrepus.vplanapp.vplan;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.SharedPrefs;

import java.util.ArrayList;

/**
 * Created by samuel on 19.08.14.
 */
public class VplanFragment extends Fragment implements View.OnClickListener {

    private SharedPreferences pref;
    private ArrayList<Row> hiddenItems;
    private int requestedVplanMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();

        //args could be corrupted if ram was cleared while in background => restart the activity
        try {
            requestedVplanMode = args.getInt(Args.VPLAN_MODE);
        } catch (Exception e) {

            //restart, something is wrong here
            getActivity().finish();
            startActivity(new Intent(getActivity(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
            return View.inflate(getActivity(), R.layout.view_card_loading, null);
        }

        int listSize = args.getInt(Args.LIST_SIZE);
        int hiddenItemsCount = args.getInt(Args.HIDDEN_ITEMS_COUNT);
        int listSizeBeforeFilter = args.getInt(Args.LIST_SIZE_ORIGINAL);

        if (pref == null) {
            pref = getActivity().getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        }

        //if requested, only display a dummy fragment

        if (args.getBoolean(Args.VPLAN_LOADING_DUMMY)) {

            return inflater.inflate(
                    R.layout.view_card_loading, container, false);
        } else {

            //The last two args ensure LayoutParams are inflated properly
            View rootView = inflater.inflate(
                    R.layout.view_card_vplan, container, false);

            //if there should be no data available after filtering, then inflate the no-data layout and mention the removed entries
            if (listSizeBeforeFilter > 0) return inflateDataLayout(inflater, container, args, listSize, hiddenItemsCount, listSizeBeforeFilter, rootView);
            else return inflateNoDataLayout(inflater, container, args);
        }
    }

    @NonNull
    private View inflateNoDataLayout(LayoutInflater inflater, ViewGroup container, Bundle args) {
        View rootView;
        rootView = inflater.inflate(
                R.layout.view_card_no_data_vplan, container, false);

        displayTimePublished(rootView, args.getInt(Args.REQUESTED_VPLAN_ID));

        TextView hiddenDataTV = (TextView) rootView.findViewById(R.id.hiddenItemsTV);
        hiddenDataTV.setText("");

        RelativeLayout hiddenDataFrame = (RelativeLayout) rootView.findViewById(R.id.hiddenDataFrame);
        hiddenDataFrame.setOnClickListener(this);
        return rootView;
    }

    @NonNull
    private View inflateDataLayout(LayoutInflater inflater, ViewGroup container, Bundle args, int listSize, int hiddenItemsCount, int listSizeBeforeFilter, View rootView) {
        if (listSize == 0) {
            rootView = inflater.inflate(
                    R.layout.view_card_no_data_vplan, container, false);
            TextView timePublishedTV = (TextView) rootView.findViewById(R.id.timeChangedTextView);
            String currTimePublished = pref.getString(SharedPrefs.PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanMode) + String.valueOf(args.getInt(Args.REQUESTED_VPLAN_ID)), "");
            timePublishedTV.setText(currTimePublished);

            //get the hidden items
            hiddenItems = ((ArrayList<Row>) args.getSerializable(Args.HIDDEN_ITEMS));
            displayHiddenItemsCount(rootView, hiddenItemsCount);

            return rootView;
        } else {
            VplanListAdapter adapter = (VplanListAdapter) args.getSerializable(Args.ADAPTER);

            //display everything
            ListView listView = (ListView) rootView.findViewById(R.id.vplanListView);

            //get hidden items and activate the onclick listener for the footer
            hiddenItems = ((ArrayList<Row>) args.getSerializable(Args.HIDDEN_ITEMS));
            addHiddenItemsCountFooter(listView, listSizeBeforeFilter - listSize);

            listView.setAdapter(adapter);

            //update textview for current timePublished
            displayTimePublished(rootView, args.getInt(Args.REQUESTED_VPLAN_ID));

            return rootView;
        }
    }

    @Override
    public void onClick(View v) {

        if (hiddenItems != null) {
            if (hiddenItems.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.hidden_items);
                builder.setAdapter(new VplanListAdapter(getActivity(), hiddenItems), null);
                builder.show();
            } //else there is no data to be shown and would cause an error
        }
    }

    private void addHiddenItemsCountFooter(ListView listView, int hiddenItemsCount) {

        //display a footer view that can be clicked in order to show hidden items; if there are no hidden items add an empty footer for a bottom padding
        View listFooter = LayoutInflater.from(getActivity()).inflate(R.layout.list_footer_vplan, null);

        String msgMode;
        if (hiddenItemsCount > 0) {

            if (hiddenItemsCount > 1) msgMode = getString(R.string.msg_hidden_plural);
            else msgMode = getString(R.string.msg_hidden_singular);

            //display the hidden items count in listview footer
            TextView hiddenItemsTV = (TextView) listFooter.findViewById(R.id.hiddenItemsTV);
            hiddenItemsTV.setText(String.valueOf(hiddenItemsCount) + " " + msgMode);
            listFooter.setOnClickListener(this);
            listView.addFooterView(listFooter);
        } else {
            TextView hiddenItemsTV = (TextView) listFooter.findViewById(R.id.hiddenItemsTV);
            hiddenItemsTV.setText("");
            listView.addFooterView(listFooter);
        }
    }

    private void displayHiddenItemsCount(View rootView, int listSizeBeforeFilter) {

        String msgMode;

        if (listSizeBeforeFilter > 1) msgMode = getString(R.string.msg_hidden_plural);
        else msgMode = getString(R.string.msg_hidden_singular);

        TextView hiddenItemsTV = (TextView) rootView.findViewById(R.id.hiddenItemsTV);
        hiddenItemsTV.setText("(" + String.valueOf(listSizeBeforeFilter) + " " + msgMode + ")");

        RelativeLayout hiddenDataFrame = (RelativeLayout) rootView.findViewById(R.id.hiddenDataFrame);
        hiddenDataFrame.setOnClickListener(this);
    }

    private void displayTimePublished(View rootView, int vplanId) {

        TextView timePublishedTV = (TextView) rootView.findViewById(R.id.timeChangedTextView);
        String currTimePublished = pref.getString(SharedPrefs.PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanMode) + String.valueOf(vplanId), "");
        timePublishedTV.setText(currTimePublished);
    }
}
