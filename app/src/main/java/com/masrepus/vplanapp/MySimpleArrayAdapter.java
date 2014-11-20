package com.masrepus.vplanapp;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by samuel on 27.07.14.
 */
public class MySimpleArrayAdapter extends ArrayAdapter implements Serializable {

    private final ArrayList<Row> list;

    /**
     * Constructor for the custom arrayadapter
     *
     * @param activity used for method-calls that require a context parameter
     * @param list     a Row ArrayList that has to be parsed into a listview
     */
    public MySimpleArrayAdapter(Activity activity, ArrayList<Row> list) {
        super(activity, R.layout.vplan_list, list);
        this.list = list;
    }

    /**
     * Puts the klasse, stunde and status attributes of a row object into the right textviews in a listview item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        ViewHolder view;

        if (rowView == null) {
            //get a new instance of the row layout view
            rowView = View.inflate(getContext(), R.layout.vplanlist_element, null);

            //hold the view objects in an object, that way they don't need to be "re- found"
            view = new ViewHolder();
            view.klasseView = (TextView) rowView.findViewById(R.id.klasse);
            view.stundeView = (TextView) rowView.findViewById(R.id.stunde);
            view.statusView = (TextView) rowView.findViewById(R.id.status);

            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        //put data to the views
        Row item = list.get(position);
        view.klasseView.setText(item.getKlasse());
        view.stundeView.setText(item.getStunde());
        view.statusView.setText(item.getStatus());

        return rowView;
    }

    /**
     * Used to distribute klasse, stunde, status to the right textviews
     */
    protected static class ViewHolder {
        protected TextView klasseView;
        protected TextView stundeView;
        protected TextView statusView;
    }
}
