package com.masrepus.vplanapp.vplan;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.masrepus.vplanapp.CollectionTools;
import com.masrepus.vplanapp.constants.Args;
import com.masrepus.vplanapp.constants.SharedPrefs;
import com.masrepus.vplanapp.constants.VplanModes;
import com.masrepus.vplanapp.databases.DataSource;
import com.masrepus.vplanapp.databases.SQLiteHelperVplan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by samuel on 19.08.14.
 */
public class VplanPagerAdapter extends FragmentStatePagerAdapter {

    private int count;
    private Context context;
    private Activity activity;
    private DataSource datasource;
    private ArrayList<String> filter;
    private ArrayList<VplanListAdapter> adapters;
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
        datasource = new DataSource(context);
        this.context = context;
        this.filter = filter;
        this.activity = activity;

        SharedPreferences pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        vplanMode = pref.getInt(SharedPrefs.VPLAN_MODE, VplanModes.UINFO);

        //get the amount of available days from database
        datasource.open();
        Cursor c = datasource.query(SQLiteHelperVplan.TABLE_LINKS, new String[]{SQLiteHelperVplan.COLUMN_ID});
        count = c.getCount();
        datasource.close();

        initData();
    }

    public boolean hasData() {
        return hasData;
    }

    public void initData() {

        adapters = new ArrayList<>(count);
        hiddenItems = new ArrayList<>(count);
        dataLists = new ArrayList<>(count);

        //fill the arraylists with null items
        for (int i = 0; i < count; i++) {
            adapters.add(null);
            hiddenItems.add(null);
            dataLists.add(null);
        }
        listSizesBeforeFilter = new int[count];

        for (int i = 0; i < count; i++) {

            //prepare the data for the adapters
            fillWithData(i);
        }

        for (int i = 0; i < count; i++) {

            //create the right adapter for each fragment's listview, if the specific data list is empty, just leave the adapter on null
            try {
                if (dataLists.size() > 0 && dataLists.get(i) != null && dataLists.get(i).size() > 0)
                    adapters.add(i, new VplanListAdapter(activity, dataLists.get(i)));
            } catch (Exception e) {
            }
        }
    }

    public VplanListAdapter getArrayAdapter(int id) {
        return adapters.get(id);
    }

    public ArrayList<Row> getHiddenItems(int id) {

        if (hiddenItems.size() > id) return hiddenItems.get(id);
        else return null;
    }

    public void fillWithData(int id) {

        datasource.open();
        String tableName = getTableName(id);

        if (datasource.hasData(tableName)) {

            //set hasData to true so that the adapter loader knows whether to disable the welcome tv or not
            hasData = true;

            //open the db cursor
            Cursor c = datasource.query(tableName, new String[]{SQLiteHelperVplan.COLUMN_ID, SQLiteHelperVplan.COLUMN_GRADE, SQLiteHelperVplan.COLUMN_STUNDE,
                    SQLiteHelperVplan.COLUMN_STATUS});

            ArrayList<Row> list = new ArrayList<>();
            ArrayList<Row> tempList = new ArrayList<>();

            //check whether filter is active
            Boolean isFilterActive = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0).getBoolean(SharedPrefs.IS_FILTER_ACTIVE, false);

            //if filter is active, then use it after filling the Arraylist
            int listSizeBeforeFilter = 0;
            if (isFilterActive) {

                tempList = getRawList(c, tempList);
                listSizesBeforeFilter[id] = tempList.size();

                //now perform the filtering
                applyFilter(id, list, tempList);

            } else {
                // just fill the list normally
                list = getRawList(c, list);

                dataLists.set(id, list);
                listSizesBeforeFilter[id] = list.size();
            }
        } else {
            dataLists.set(id, null);
            try {
                hiddenItems.set(id, null);
            } catch (Exception e) {
            }
        }
        datasource.close();
    }

    private ArrayList<Row> getRawList(Cursor c, ArrayList<Row> tempList) {
        while (c.moveToNext()) {
            Row row = new Row();

            //only add to list if row isn't null
            String help = c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE));
            if (help.contentEquals("Klasse")) continue;
            if (help.contentEquals("")) continue;

            row.setKlasse(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_GRADE)));
            row.setStunde(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STUNDE)));
            row.setStatus(c.getString(c.getColumnIndex(SQLiteHelperVplan.COLUMN_STATUS)));

            tempList.add(row);
        }

        return tempList;
    }

    @NonNull
    private String getTableName(int id) {
        //query the data for the right vplan -> get requested table name by passed arg
        String tableName;

        switch (id) {

            case 0:
                tableName = SQLiteHelperVplan.TABLE_VPLAN_0;
                break;
            case 1:
                tableName = SQLiteHelperVplan.TABLE_VPLAN_1;
                break;
            case 2:
                tableName = SQLiteHelperVplan.TABLE_VPLAN_2;
                break;
            case 3:
                tableName = SQLiteHelperVplan.TABLE_VPLAN_3;
                break;
            case 4:
                tableName = SQLiteHelperVplan.TABLE_VPLAN_4;
                break;
            default:
                tableName = SQLiteHelperVplan.TABLE_VPLAN_0;
                break;
        }
        return tableName;
    }

    private void applyFilter(int id, ArrayList<Row> list, ArrayList<Row> tempList) {
        for (Row currRow : tempList) {
            String klasse = currRow.getKlasse();
            boolean isNeeded = false;

            //look whether this row's klasse attribute contains any of the classes to filter for
            if (filter.size() > 0) {
                for (int i = 0; i <= filter.size() - 1; i++) {
                    char[] klasseFilter = filter.get(i).toCharArray();

                    //check whether this is oinfo, as in this case, the exact order of the filter chars must be given as well
                    if (vplanMode == VplanModes.OINFO) {
                        String filterItem = filter.get(i);
                        isNeeded = klasse.contentEquals("Q" + filterItem);

                        //some courses with e.g. Q1Inf1 can also sometimes be called Q1Inf. So look for those cases
                        if (filterItem.charAt(filterItem.length()-1) == '1') isNeeded = (klasse + "1").contentEquals("Q" + filterItem);

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
        dataLists.set(id, list);
        hiddenItems.set(id, new ArrayList<>(CollectionTools.nonOverLap(tempList, list)));
        listSizesBeforeFilter[id] = tempList.size();
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
        args.putInt(Args.REQUESTED_VPLAN_ID, i);
        args.putInt(Args.VPLAN_MODE, vplanMode);
        if (adapters.size() > i) {
            if (adapters.get(i) != null) {
                args.putSerializable(Args.ADAPTER, adapters.get(i));
            }
        }
        if (hiddenItems.size() > i) {
            if (hiddenItems.get(i) != null) {
                args.putInt(Args.HIDDEN_ITEMS_COUNT, hiddenItems.get(i).size());
                args.putSerializable(Args.HIDDEN_ITEMS, hiddenItems.get(i));
            }
        } else args.putInt(Args.HIDDEN_ITEMS_COUNT, 0);

        if (dataLists.size() > i) {
            if (dataLists.get(i) != null) {
                args.putInt(Args.LIST_SIZE, dataLists.get(i).size());
            }
        } else args.putInt(Args.LIST_SIZE, 0);

        if (listSizesBeforeFilter.length > i)
            args.putInt(Args.LIST_SIZE_ORIGINAL, listSizesBeforeFilter[i]);
        else args.putInt(Args.LIST_SIZE_ORIGINAL, 0);
        args.putBoolean(Args.VPLAN_LOADING_DUMMY, false);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public int getCount() {
        return count;
    }

    /**
     * Gets the page title from shared prefs by using the position as current vplan id
     */
    @Override
    public CharSequence getPageTitle(int position) {

        CharSequence title;

        //this vplan's current date is used as title
        SharedPreferences prefs = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        title = prefs.getString(SharedPrefs.PREFIX_VPLAN_CURR_DATE + String.valueOf(vplanMode) + String.valueOf(position), "");

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat hour = new SimpleDateFormat("HH");
        saveNextVplanDate(position, title, prefs, calendar, format, hour);

        return title;
    }

    private void saveNextVplanDate(int position, CharSequence title, SharedPreferences prefs, Calendar calendar, SimpleDateFormat format, SimpleDateFormat hour) {
        //save the position of today's vplan in shared prefs
        if ((String.valueOf(title)).contains(format.format(calendar.getTime())) && Integer.valueOf(hour.format(calendar.getTime())) < 17) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(SharedPrefs.TODAY_VPLAN, position);
            editor.apply();
        } else if (Integer.valueOf(hour.format(calendar.getTime())) >= 17) {
            calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) >= Calendar.FRIDAY) {
                int daysToMonday = (Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) + 2) % 7;
                calendar.add(Calendar.DAY_OF_MONTH, daysToMonday);
            } else calendar.add(Calendar.DAY_OF_MONTH, 1);
            if ((String.valueOf(title)).contains(format.format(calendar.getTime()))) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(SharedPrefs.TODAY_VPLAN, position);
                editor.apply();
            }
        }
    }
}
