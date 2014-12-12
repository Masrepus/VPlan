package com.masrepus.vplanapp;

import android.content.Context;

import com.masrepus.vplanapp.constants.VplanModes;

import java.util.ArrayList;

/**
 * Created by samuel on 31.10.14.
 */
public class DrawerListBuilder {

    private String[] headers;
    private ArrayList<Integer> separatorPositions;
    private ArrayList<Item> items;
    private Context context;

    /**
     * @param headers all the header strings
     * @param entries all the entry strings
     * @param separateSectionsBefore indices where section headers should be inserted before that
     */
    public DrawerListBuilder(Context context, String[] headers, String[] entries, int... separateSectionsBefore) {
        this.context = context;
        this.headers = headers;

        separatorPositions = new ArrayList<Integer>();
        for (int i = 0; i < separateSectionsBefore.length; i++) {
            separatorPositions.add(i, separateSectionsBefore[i]);
        }

        buildItemList(entries);
    }

    public static String[] addArrays(String[]... arrays) {

        int finalSize = 0;
        for (String[] array : arrays) {
            finalSize += array.length;
        }

        //create a new array of the total size and fill it with all the entries
        String[] result = new String[finalSize];
        int position = 0;
        for (String[] array : arrays) {

            for (String string : array) {
                result[position] = string;
                position++;
            }
        }

        return result;
    }

    public void buildItemList(String[] entries) {

        int headersPos = 0;
        items = new ArrayList<Item>();

        for (int i=0; i<entries.length; i++) {

            //check whether a new block starts at the current position
            if (separatorPositions.contains(i)) {
                //add a new section header item
                items.add(new SectionItem(headers[headersPos]));
                headersPos++;
            }
            //add the current entry item
            String title = entries[i];
            int vplanMode = 0;
            boolean isVplanMode = false;
            if (title.contentEquals(context.getString(R.string.unterstufe))) {
                isVplanMode = true;
                vplanMode = VplanModes.UINFO;
            } else if (title.contentEquals(context.getString(R.string.mittelstufe))) {
                isVplanMode = true;
                vplanMode = VplanModes.MINFO;
            } else if (title.contentEquals(context.getString(R.string.oberstufe))) {
                isVplanMode = true;
                vplanMode = VplanModes.OINFO;
            }

            if (isVplanMode) {
                //this is a vplan mode entry
                items.add(new EntryItem(title, vplanMode));
            } else items.add(new EntryItem(title));
        }
    }

    public ArrayList<Item> getItems() {
        return items;
    }
}
