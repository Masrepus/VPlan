package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by samuel on 19.08.14.
 */
public class VplanPagerAdapter extends FragmentStatePagerAdapter {

    private Context context;
    private Activity activity;
    private VPlanDataSource datasource;
    private ArrayList<String> filter;
    private ArrayList<MySimpleArrayAdapter> adapters;
    private ArrayList<ArrayList<Row>> hiddenItems;
    private ArrayList<ArrayList<Row>> dataLists;
    private int[] listSizesBeforeFilter;
    private int vplanMode;
    private boolean hasData;

    /**
     * Initialisation of the datasource using the passed context and saving the passed filter arraylist as a local variable
     */
    public VplanPagerAdapter(FragmentManager fm, Context context, Activity activity, ArrayList<String> filter) {
        super(fm);
        datasource = new VPlanDataSource(context);
        this.context = context;
        this.filter = filter;
        this.activity = activity;

        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        vplanMode = pref.getInt(MainActivity.PREF_VPLAN_MODE, MainActivity.UINFO);

        initData();
    }

    public boolean hasData() {
        return hasData;
    }

    public void initData() {

        int count = getCount();
        adapters = new ArrayList<MySimpleArrayAdapter>(count);
        hiddenItems = new ArrayList<ArrayList<Row>>(count);
        dataLists = new ArrayList<ArrayList<Row>>(count);
        listSizesBeforeFilter = new int[count];

        for (int i=0; i<count; i++) {

            //prepare the data for the adapters
            fillWithData(i);
        }

        for (int i=0; i<count; i++) {

            //create the right adapter for each fragment's listview, if the specific data list is empty, just leave the adapter on null
            try {
                if (dataLists.size() > 0 && dataLists.get(i) != null && dataLists.get(i).size() > 0)
                    adapters.add(i, new MySimpleArrayAdapter(activity, dataLists.get(i)));
            } catch(Exception e) {}
        }
    }

    public MySimpleArrayAdapter getArrayAdapter(int id) {
        return adapters.get(id);
    }

    public ArrayList<Row> getHiddenItems(int id) {

        if (hiddenItems.size() > id) return hiddenItems.get(id);
        else return null;
    }

    public void fillWithData(int id) {

        datasource.open();

        //query the data for the right vplan -> get requested table name by passed arg
        String tableName;

        switch (id) {

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

            //set hasData to true so that the adapter loader knows whether to disable the welcome tv or not
            hasData = true;

            Cursor c = datasource.query(tableName, new String[]{MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_KLASSE, MySQLiteHelper.COLUMN_STUNDE,
                    MySQLiteHelper.COLUMN_STATUS});

            ArrayList<Row> list = new ArrayList<Row>();
            ArrayList<Row> tempList = new ArrayList<Row>();

            //check whether filter is active
            Boolean isFilterActive = context.getSharedPreferences(MainActivity.PREFS_NAME, 0).getBoolean(MainActivity.PREF_IS_FILTER_ACTIVE, false);

            //if filter is active, then use it after filling the Arraylist
            int listSizeBeforeFilter = 0;
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

                listSizesBeforeFilter[id] = tempList.size();

                //now perform the filtering
                for (Row currRow : tempList) {
                    String klasse = currRow.getKlasse();
                    boolean isNeeded = false;

                    //look whether this row's klasse attribute contains any of the classes to filter for
                    if (filter.size() > 0) {
                        for (int i = 0; i <= filter.size() - 1; i++) {
                            char[] klasseFilter = filter.get(i).toCharArray();

                            //check whether this is oinfo, as in this case, the exact order of the filter chars must be given as well
                            if (vplanMode == MainActivity.OINFO) {
                                String filterItem = filter.get(i);
                                if (vplanMode != MainActivity.OINFO)isNeeded = klasse.contains(filterItem);
                                else isNeeded = klasse.contentEquals("Q" + filterItem);

                                if (isNeeded) break;
                                if (klasse.contentEquals("")) isNeeded = true;
                            } else { //in u/minfo the order doesn't play a role

                                //if klasse contains all of the characters of the filter string, isNeeded will be true, because if one character returns false, the loop is stopped
                                for (int y = 0; y <= klasseFilter.length - 1; y++) {
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
                        }
                    } else {
                        //if there is no item in the filter list, then still take the rows without a value for class
                        isNeeded = klasse.contentEquals("");
                    }
                    //if the test was positive, then add the current Row to the list
                    if (isNeeded) list.add(currRow);
                }

                //now save the differences in the hiddenItems list
                dataLists.add(id, list);
                hiddenItems.add(id, new ArrayList<Row>(nonOverLap(tempList, list)));
                listSizesBeforeFilter[id] = tempList.size();

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

                dataLists.add(id, list);
                listSizesBeforeFilter[id] = list.size();
            }
        } else {
            dataLists.add(id, null);
            hiddenItems.add(id, null);
        }
        datasource.close();
    }

    /**
     * Requests a new VPlanFragment by passing the current filter and the requested vplan id
     *
     * @param i used as requested vplan id
     */
    @Override
    public Fragment getItem(int i) {

        Fragment fragment = new VplanFragment();
        Bundle args = new Bundle();

        //the id of the requested vplan for this fragment is the fragment's id; also pass hidden items count, data list size, original list size before filtering
        args.putInt(VplanFragment.ARG_REQUESTED_VPLAN_ID, i);
        args.putInt(VplanFragment.ARG_VPLAN_MODE, vplanMode);
        if (adapters.size() > i) {
            if (adapters.get(i) != null) {
                args.putSerializable(VplanFragment.ARG_ADAPTER, adapters.get(i));
            }
        }
        if (hiddenItems.size() > i) {
            if (hiddenItems.get(i) != null) {
                args.putInt(VplanFragment.ARG_HIDDEN_ITEMS_COUNT, hiddenItems.get(i).size());
                args.putSerializable(VplanFragment.ARG_HIDDEN_ITEMS, hiddenItems.get(i));
            }
        } else args.putInt(VplanFragment.ARG_HIDDEN_ITEMS_COUNT, 0);

        if (dataLists.size() > i) {
            if (dataLists.get(i) != null) {
                args.putInt(VplanFragment.ARG_LIST_SIZE, dataLists.get(i).size());
            }
        } else args.putInt(VplanFragment.ARG_LIST_SIZE, 0);

        if (listSizesBeforeFilter.length > i) args.putInt(VplanFragment.ARG_LIST_SIZE_ORIGINAL, listSizesBeforeFilter[i]);
        else args.putInt(VplanFragment.ARG_LIST_SIZE_ORIGINAL, 0);
        args.putBoolean(VplanFragment.FLAG_VPLAN_LOADING_DUMMY, false);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Gets the amount of available days from the db and returns this value
     */
    @Override
    public int getCount() {

        //get the amount of available days from database
        datasource.open();
        Cursor c = datasource.query(MySQLiteHelper.TABLE_LINKS, new String[]{MySQLiteHelper.COLUMN_ID});
        int count = c.getCount();
        datasource.close();

        return count;
    }

    /**
     * Gets the page title from shared prefs by using the position as current vplan id
     */
    @Override
    public CharSequence getPageTitle(int position) {

        CharSequence title;

        //this vplan's current date is used as title
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        title = prefs.getString(MainActivity.PREF_PREFIX_VPLAN_CURR_DATE + String.valueOf(vplanMode) + String.valueOf(position), "");

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat hour = new SimpleDateFormat("HH");

        //save the position of today's vplan in shared prefs
        if ((String.valueOf(title)).contains(format.format(calendar.getTime())) && Integer.valueOf(hour.format(calendar.getTime())) < 17) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(MainActivity.PREF_TODAY_VPLAN, position);
            editor.apply();
        } else if (Integer.valueOf(hour.format(calendar.getTime())) >= 17) {
            calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) >= 6 /*friday*/) {
                calendar.add(Calendar.DAY_OF_MONTH, getDaysUntilMonday(calendar));
            } else calendar.add(Calendar.DAY_OF_MONTH, 1);
            if ((String.valueOf(title)).contains(format.format(calendar.getTime()))) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(MainActivity.PREF_TODAY_VPLAN, position);
                editor.apply();
            }
        }

        return title;
    }

    private int getDaysUntilMonday(Calendar calendar) {

        return 3 - Math.abs(calendar.get(Calendar.DAY_OF_WEEK) - 6);
    }

    private Collection<Row> union(Collection<Row> coll1, Collection<Row> coll2) {
        Set<Row> union = new HashSet<Row>(coll1);
        union.addAll(new HashSet<Row>(coll2));
        return union;
    }

    private Collection<Row> intersect(Collection<Row> coll1, Collection<Row> coll2) {
        Set<Row> intersection = new HashSet<Row>(coll1);
        intersection.retainAll(new HashSet<Row>(coll2));

        return intersection;
    }

    /**
     * Finds out the differences between two collections
     *
     * @return returns a collection containing the items that are different
     */
    private Collection<Row> nonOverLap(Collection<Row> coll1, Collection<Row> coll2) {
        Collection<Row> result = union(coll1, coll2);
        result.removeAll(intersect(coll1, coll2));
        return result;
    }
}
