package com.masrepus.vplanapp;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

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
