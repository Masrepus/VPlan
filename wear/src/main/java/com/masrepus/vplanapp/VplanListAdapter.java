package com.masrepus.vplanapp;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by samuel on 27.07.14.
 */
public class VplanListAdapter extends WearableListView.Adapter {

    private ArrayList<Row> rows;
    private final Context context;
    private final LayoutInflater inflater;

    public VplanListAdapter(Context context, ArrayList<Row> rows) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.rows = rows;

        //sort the rows depending on their lesson
        Collections.sort(this.rows, new Comparator<Row>() {
            @Override
            public int compare(Row row, Row row2) {
                return row.getStunde().compareTo(row2.getStunde());
            }
        });
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //create a new instance of the custom viewholder
        return new ItemViewHolder(inflater.inflate(R.layout.vplanlist_item, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {

        //get the textviews
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView hour = itemHolder.hour;
        TextView grade = itemHolder.grade;
        TextView status = itemHolder.status;

        //set texts
        hour.setText(rows.get(position).getStunde());
        grade.setText(rows.get(position).getKlasse());
        status.setText(rows.get(position).getStatus());

        //replace the list item's metadata
        itemHolder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public static class ItemViewHolder extends WearableListView.ViewHolder {


        private TextView hour;
        private TextView grade;
        private TextView status;

        public ItemViewHolder(View itemView) {
            super(itemView);

            //init the views
            hour = (TextView) itemView.findViewById(R.id.hour);
            grade = (TextView) itemView.findViewById(R.id.grade);
            status = (TextView) itemView.findViewById(R.id.status);
        }
    }
}
