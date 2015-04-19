package com.masrepus.vplanapp.drawer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.masrepus.vplanapp.exams.ExamsActivity;
import com.masrepus.vplanapp.vplan.MainActivity;
import com.masrepus.vplanapp.R;
import com.masrepus.vplanapp.timetable.TimetableActivity;
import com.masrepus.vplanapp.constants.AppModes;
import com.masrepus.vplanapp.constants.SharedPrefs;

import java.util.ArrayList;

/**
 * Created by samuel on 07.12.14.
 */
 public class DrawerListAdapter extends ArrayAdapter<Item> {

    private Context context;
    private Activity activity;
    private ArrayList<Item> items;
    private SharedPreferences pref;
    private int selectedVplanItem;
    private int selectedAppmodeItem;

    public DrawerListAdapter(Activity activity, Context context, ArrayList<Item> items) {
        super(context, 0, items);
        this.activity = activity;
        this.context = context;
        this.items = items;
        pref = context.getSharedPreferences(SharedPrefs.PREFS_NAME, 0);
        selectedAppmodeItem = pref.getInt(SharedPrefs.SELECTED_APPMODE_ITEM, 0);
        selectedVplanItem = pref.getInt(SharedPrefs.SELECTED_APPMODE_ITEM, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder view;
        final Item i = items.get(position);

        if (i != null) {
            if (i.isSection()) {
                //this is a header item
                SectionItem section = (SectionItem) i;

                if (convertView == null) {
                    view = new ViewHolder(true);
                    convertView = View.inflate(context, R.layout.section_item, null);
                    view.titleView = (TextView) convertView.findViewById(R.id.title);
                } else view = (ViewHolder) convertView.getTag(R.id.TAG_VIEWHOLDER);

                //if this was an entry item before, re-inflate and update viewholder
                if (!view.isSection) {
                    convertView = View.inflate(context, R.layout.section_item, null);
                    view = new ViewHolder(true);
                    view.titleView = (TextView) convertView.findViewById(R.id.title);
                }

                view.titleView.setText(section.getTitle());
                convertView.setOnClickListener(null);

                convertView.setTag(R.id.TAG_VIEWHOLDER, view);
            } else {
                EntryItem entry = (EntryItem) i;

                if (convertView == null) {
                    view = new ViewHolder(false);
                    convertView = View.inflate(context, R.layout.drawer_list_item, null);
                } else view = (ViewHolder) convertView.getTag(R.id.TAG_VIEWHOLDER);

                //if this was a section item before, re-inflate and update viewholder
                if (view.isSection) {
                    convertView = View.inflate(context, R.layout.drawer_list_item, null);
                    view = new ViewHolder(false);
                }

                view.titleView = (TextView) convertView.findViewById(R.id.title);
                view.titleView.setText(entry.getTitle());
                convertView.setClickable(true);

                //check which activity is onClick listener now
                if (activity instanceof MainActivity) {
                    convertView.setOnClickListener(((MainActivity) activity));
                } else if (activity instanceof ExamsActivity) {
                    convertView.setOnClickListener(((ExamsActivity) activity));
                } else if (activity instanceof TimetableActivity) {
                    convertView.setOnClickListener((TimetableActivity) activity);
                }

                //if this item has a vplan mode attached to it, add it as a tag
                if (entry.isVplanMode()) {
                    convertView.setTag(R.id.TAG_VPLAN_MODE, entry.getVplanMode());
                } else {

                    //set the appmode tag according to title
                    int appmodeTag;
                    if (entry.getTitle().contentEquals(context.getString(R.string.substitutions)))
                        appmodeTag = AppModes.VPLAN;
                    else if (entry.getTitle().contentEquals(context.getString(R.string.timetable)))
                        appmodeTag = AppModes.TIMETABLE;
                    else appmodeTag = AppModes.TESTS;

                    convertView.setTag(R.id.TAG_APPMODE, appmodeTag);
                }

                convertView.setTag(R.id.TAG_POSITION, position);

                //if this view is selected, change the background color
                if (selectedVplanItem == position || selectedAppmodeItem == position) {
                    convertView.setBackgroundColor(context.getResources().getColor(R.color.yellow));
                    view.titleView.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    convertView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                    view.titleView.setTypeface(Typeface.DEFAULT);
                }

                convertView.setTag(R.id.TAG_VIEWHOLDER, view);
            }
        }

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        //get the new selected items
        selectedAppmodeItem = pref.getInt(SharedPrefs.SELECTED_APPMODE_ITEM, 0);
        selectedVplanItem = pref.getInt(SharedPrefs.SELECTED_VPLAN_ITEM, 0);
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    protected class ViewHolder {
        protected TextView titleView;
        protected boolean isSection;

        public ViewHolder(boolean isSection) {
            this.isSection = isSection;
        }
    }
}
