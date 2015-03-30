package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetableListAdapter extends ArrayAdapter implements Serializable {

    private ArrayList<TimetableRow> rows;
    private TimetableActivity activity;

    public TimetableListAdapter(TimetableActivity activity, ArrayList<TimetableRow> rows) {
        super(activity, R.layout.timetable_item, rows);
        this.rows = rows;
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        ViewHolder view;

        //put data to the views

        //check if this is a regular lesson row or a break row
        TimetableRow item = rows.get(position);

        if (item instanceof BreakRow) {

            if (rowView == null) {

                //get a new instance of the footer layout view to display the break info
                rowView = View.inflate(getContext(), R.layout.break_view, null);

                view = new ViewHolder();
                view.isBreakItem = true;
                rowView.setTag(view);
            } else {

                view = (ViewHolder) rowView.getTag();

                if (!view.isBreakItem) {

                    //get a new instance of the footer layout view to display the break info
                    rowView = View.inflate(getContext(), R.layout.break_view, null);

                    view = new ViewHolder();
                    view.isBreakItem = true;
                    rowView.setTag(view);
                }
            }
        } else {

            //if this is an empty view or a break view, re-inflate the correct layout
            if (rowView == null) {
                //get a new instance of the row layout view
                rowView = View.inflate(getContext(), R.layout.timetable_item, null);

                //hold the view objects in an object, that way they don't need to be "re-found"
                view = new ViewHolder();
                view.lesson = (TextView) rowView.findViewById(R.id.lesson);
                view.subject = (TextView) rowView.findViewById(R.id.subject);
                view.room = (TextView) rowView.findViewById(R.id.room);
                view.isBreakItem = false;

                rowView.setTag(view);
            } else {

                view = (ViewHolder) rowView.getTag();

                if (view.isBreakItem) {

                    //re-inflate as this is a wrong item
                    //get a new instance of the row layout view
                    rowView = View.inflate(getContext(), R.layout.timetable_item, null);

                    //hold the view objects in an object, that way they don't need to be "re-found"
                    view = new ViewHolder();
                    view.lesson = (TextView) rowView.findViewById(R.id.lesson);
                    view.subject = (TextView) rowView.findViewById(R.id.subject);
                    view.room = (TextView) rowView.findViewById(R.id.room);
                    view.isBreakItem = false;

                    rowView.setTag(view);
                }
            }

            view.lesson.setText(item.getLesson());
            view.subject.setText(item.getSubject());
            view.room.setText(item.getRoom());

            //let the timetable activity handle long clicks
            rowView.setOnLongClickListener(activity);
        }

        return rowView;
    }

    protected static class ViewHolder {
        protected TextView lesson;
        protected TextView subject;
        protected TextView room;
        protected boolean isBreakItem;
    }
}
