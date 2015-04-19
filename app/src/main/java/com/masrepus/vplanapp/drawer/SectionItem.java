package com.masrepus.vplanapp.drawer;

/**
 * Section header item
 * Created by samuel on 30.10.14.
 */
public class SectionItem implements Item {

    public final String title;

    public SectionItem(String title) {

        this.title = title;
    }

    @Override
    public boolean isSection() {
        return true;
    }

    public String getTitle() {
        return title;
    }
}
