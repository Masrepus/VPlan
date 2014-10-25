package com.masrepus.vplanapp;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by samuel on 19.08.14.
 */
public class VplanFragment extends Fragment implements View.OnClickListener {

    public static final String ARG_REQUESTED_VPLAN_ID = "requestedVplan";
    public static final String ARG_VPLAN_MODE = "vplanmode";
    public static final String FLAG_VPLAN_LOADING_DUMMY = "loadingDummy";
    public static final String ARG_LIST_SIZE = "size";
    public static final String ARG_HIDDEN_ITEMS_COUNT = "hiddenCount";
    public static final String ARG_LIST_SIZE_ORIGINAL = "listSizeBeforeFilter";
    private SharedPreferences pref;
    private ArrayList<Row> hiddenItems;
    private int requestedVplanMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle args = getArguments();
        int id = args.getInt(ARG_REQUESTED_VPLAN_ID);
        requestedVplanMode = args.getInt(ARG_VPLAN_MODE);
        int listSize = args.getInt(ARG_LIST_SIZE);
        int hiddenItemsCount = args.getInt(ARG_HIDDEN_ITEMS_COUNT);
        int listSizeBeforeFilter = args.getInt(ARG_LIST_SIZE_ORIGINAL);

        if (pref == null) {
            pref = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        }
        TextView welcome = (TextView) getActivity().findViewById(R.id.welcome_textView);
        welcome.setVisibility(View.GONE);

        //if the data is still loading then only display a dummy fragment
        if (args.getBoolean(FLAG_VPLAN_LOADING_DUMMY)) {

            View rootView = inflater.inflate(
                    R.layout.vplan_loading_dummy, container, false);
            MainActivity.inflateStatus = 1;
            return rootView;
        } else {

            //The last two args ensure LayoutParams are inflated properly
            View rootView = inflater.inflate(
                    R.layout.vplan_list, container, false);

            ViewPager pager = (ViewPager) getActivity().findViewById(R.id.pager);
            VplanPagerAdapter pagerAdapter = (VplanPagerAdapter) pager.getAdapter();

                //if there should be no data available after filtering, then inflate the no-data layout and mention the removed entries
            if (listSizeBeforeFilter > 0) {
                if (listSize == 0) {
                    rootView = inflater.inflate(
                            R.layout.no_data_vplan_list, container, false);
                    TextView timePublishedTV = (TextView) rootView.findViewById(R.id.timeChangedTextView);
                    String currTimePublished = pref.getString(MainActivity.PREF_PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanMode) + String.valueOf(args.getInt(ARG_REQUESTED_VPLAN_ID)), "");
                    timePublishedTV.setText(currTimePublished);

                    //get the hidden items
                    hiddenItems = pagerAdapter.getHiddenItems(id);
                    displayHiddenItemsCount(rootView, hiddenItemsCount);

                    MainActivity.inflateStatus = 1;
                    return rootView;
                } else {

                    MySimpleArrayAdapter adapter = pagerAdapter.getArrayAdapter(id);


                    //display everything
                    ListView listView = (ListView) rootView.findViewById(R.id.vplanListView);

                    //get hidden items and activate the onclick listener for the footer
                    hiddenItems = pagerAdapter.getHiddenItems(id);
                    addHiddenItemsCountFooter(listView, listSizeBeforeFilter - listSize);

                    listView.setAdapter(adapter);

                    //update textview for current timePublished
                    displayTimePublished(rootView, args.getInt(ARG_REQUESTED_VPLAN_ID));

                    return rootView;
                }
            } else {

                rootView = inflater.inflate(
                        R.layout.no_data_vplan_list, container, false);

                displayTimePublished(rootView, args.getInt(ARG_REQUESTED_VPLAN_ID));

                TextView hiddenDataTV = (TextView) rootView.findViewById(R.id.hiddenItemsTV);
                hiddenDataTV.setText("");

                RelativeLayout hiddenDataFrame = (RelativeLayout) rootView.findViewById(R.id.hiddenDataFrame);
                hiddenDataFrame.setOnClickListener(this);
                return rootView;
            }
        }
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.hidden_items);
        builder.setAdapter(new MySimpleArrayAdapter(getActivity(), hiddenItems), null);
        builder.show();
    }

    private void addHiddenItemsCountFooter(ListView listView, int hiddenItemsCount) {

        //display a footer view that can be clicked in order to show hidden items, but only if there are any
        View listFooter = LayoutInflater.from(getActivity()).inflate(R.layout.vplan_list_footer, null);

        String msgMode;
        if (hiddenItemsCount > 0) {

            if (hiddenItemsCount > 1) msgMode = getString(R.string.msg_hidden_plural);
            else msgMode = getString(R.string.msg_hidden_singular);

            //display the hidden items count in listview footer
            TextView hiddenItemsTV = (TextView) listFooter.findViewById(R.id.hiddenItemsTV);
            hiddenItemsTV.setText(String.valueOf(hiddenItemsCount) + " " + msgMode);
            listFooter.setOnClickListener(this);
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
        String currTimePublished = pref.getString(MainActivity.PREF_PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(requestedVplanMode) + String.valueOf(vplanId), "");
        timePublishedTV.setText(currTimePublished);
    }
}
