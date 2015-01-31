package com.masrepus.vplanapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetableListAdapter extends ArrayAdapter implements Serializable {

    private ArrayList<TimetableRow> rows;

    public TimetableListAdapter(Context context, ArrayList<TimetableRow> rows) {
        super(context, R.layout.timetable_item, rows);
        this.rows = rows;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        ViewHolder view;

        if (rowView == null) {
            //get a new instance of the row layout view
            rowView = View.inflate(getContext(), R.layout.timetable_item, null);

            //hold the view objects in an object, that way they don't need to be "re- found"
            view = new ViewHolder();
            view.lesson = (TextView) rowView.findViewById(R.id.lesson);
            view.subject = (TextView) rowView.findViewById(R.id.subject);
            view.room = (TextView) rowView.findViewById(R.id.room);

            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        //put data to the views
        TimetableRow item = rows.get(position);
        view.lesson.setText(item.getLesson());
        view.subject.setText(item.getSubject());
        view.room.setText(item.getRoom());

        return rowView;
    }

    protected static class ViewHolder {
        protected TextView lesson;
        protected TextView subject;
        protected TextView room;
    }
}
