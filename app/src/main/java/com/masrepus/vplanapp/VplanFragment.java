package com.masrepus.vplanapp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by samuel on 19.08.14.
 */
public class VplanFragment extends Fragment {

    public static final String ARG_REQUESTED_VPLAN_ID = "requestedVplan";
    public static final String FLAG_VPLAN_LOADING_DUMMY = "loadingDummy";
    public static final String ARG_FILTER = "filter";
    private VPlanDataSource datasource;
    private SharedPreferences pref;
    private ArrayList<String> filter;

    /**
     * Builds a new Fragment including either a loading dummy, one displaying that there is no data available, or, if there is data to display, passes that to MySimpleArrayAdapter in order to display it in the listview of vplan_list.xml
     * @return the finished Fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle args = getArguments();
        filter = args.getStringArrayList(ARG_FILTER);
        if (pref == null) {
            pref = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        }

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

            //create an adapter that will be used to display the loaded data
            //datasource.query is passed for cursor
            datasource = new VPlanDataSource(getActivity());
            datasource.open();

            //query the data for the right vplan -> get requested table name by passed arg
            String tableName;

            switch (args.getInt(ARG_REQUESTED_VPLAN_ID)) {

                case 0:
                    tableName = MySQLiteHelper.TABLE_VPLAN_0;
                    break;
                case 1:
                    tableName = MySQLiteHelper.TABLE_VPLAN_1;
                    break;
                case 2:
                    tableName = MySQLiteHelper.TABLE_VPLAN_2;
                    break;
                case 3:
                    tableName = MySQLiteHelper.TABLE_VPLAN_3;
                    break;
                case 4:
                    tableName = MySQLiteHelper.TABLE_VPLAN_4;
                    break;
                default:
                    tableName = MySQLiteHelper.TABLE_VPLAN_0;
                    break;
            }

            if (datasource.hasData(tableName)) {
                Cursor c = datasource.query(tableName, new String[]{MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_KLASSE, MySQLiteHelper.COLUMN_STUNDE,
                        MySQLiteHelper.COLUMN_STATUS});

                ArrayList<Row> list = new ArrayList<Row>();
                ArrayList<Row> tempList = new ArrayList<Row>();

                //check whether filter is active
                Boolean isFilterActive = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0).getBoolean(MainActivity.PREF_IS_FILTER_ACTIVE, false);

                //if filter is active, then use it after filling the Arraylist
                if (isFilterActive) {

                    while (c.moveToNext()) {
                        Row row = new Row();

                        //only add to list if row isn't null
                        String help = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_KLASSE));
                        if (help == "Klasse") continue;
                        if (help == "") continue;

                        row.setKlasse(c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_KLASSE)));
                        row.setStunde(c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_STUNDE)));
                        row.setStatus(c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_STATUS)));

                        tempList.add(row);
                    }

                    //now perform the filtering
                    for (Row currRow : tempList) {
                        String klasse = currRow.getKlasse();
                        boolean isNeeded = false;

                        //look whether this row's klasse attribute contains any of the classes to filter for
                        if (filter.size() > 0) {
                            for (int i = 0; i <= filter.size() - 1; i++) {
                                char[] klasseFilter = filter.get(i).toCharArray();

                                //if klasse contains all of the characters of the filter string, isNeeded will be true, because if one character returns false, the loop is stopped
                                for (int y=0; y <= klasseFilter.length - 1; y++) {
                                    if (klasse.contains(String.valueOf(klasseFilter[y]))) {
                                        isNeeded = true;
                                    } else {
                                        isNeeded = false;
                                        break;
                                    }
                                }
                                if (isNeeded) break;

                                //also set isneeded to true if klasse=""
                                if (klasse.contentEquals("")) isNeeded = true;
                            }
                        } else {
                            //if there is no item in the filter list, then still take the rows without a value for class
                            isNeeded = klasse.contentEquals("");
                        }
                        //if the test was positive, then add the current Row to the list
                        if (isNeeded) list.add(currRow);
                    }

                } else {
                    // just fill the list normally
                    while (c.moveToNext()) {
                        Row row = new Row();

                        //only add to list if row isn't null
                        String help = c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_KLASSE));
                        if (help == "Klasse") continue;
                        if (help == "") continue;

                        row.setKlasse(c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_KLASSE)));
                        row.setStunde(c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_STUNDE)));
                        row.setStatus(c.getString(c.getColumnIndex(MySQLiteHelper.COLUMN_STATUS)));

                        list.add(row);

                    }
                }

                //if there should be no data available after filtering, then inflate the no-data layout
                if (list.size() == 0) {
                    rootView = inflater.inflate(
                            R.layout.no_data_vplan_list, container, false);
                    MainActivity.inflateStatus = 1;
                    return rootView;
                } else {

                    MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(getActivity(), list);


                    //display everything
                    ListView listView = (ListView) rootView.findViewById(R.id.vplanListView);
                    listView.setAdapter(adapter);

                    //update textview for current timePublished
                    TextView timePublishedTV = (TextView) rootView.findViewById(R.id.timeChangedTextView);
                    String currTimePublished = pref.getString(MainActivity.PREF_PREFIX_VPLAN_TIME_PUBLISHED + String.valueOf(args.getInt(ARG_REQUESTED_VPLAN_ID)), "");
                    timePublishedTV.setText(currTimePublished);

                    datasource.close();
                    return rootView;
                }
            } else {
                datasource.close();
                return inflater.inflate(R.layout.no_data_vplan_list, container, false);
            }
        }
    }
}
