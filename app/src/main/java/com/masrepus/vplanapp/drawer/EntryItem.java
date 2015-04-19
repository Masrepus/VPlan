package com.masrepus.vplanapp.drawer;

/**
 * Normal list entry item
 * Created by samuel on 30.10.14.
 */
public class EntryItem implements Item {

    public final String title;
    public final int vplanMode;

    public EntryItem(String title) {

        this.title = title;
        vplanMode = 0;
    }

    public EntryItem(String title, int vplanMode) {

        this.title = title;
        this.vplanMode = vplanMode;
    }

    @Override
    public boolean isSection() {
        return false;
    }

    public String getTitle() {
        return title;
    }

    public boolean isVplanMode() {
        return vplanMode != 0;
    }

    public int getVplanMode() {
        return vplanMode;
    }
}
