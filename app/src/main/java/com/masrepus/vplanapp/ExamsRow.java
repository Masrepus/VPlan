package com.masrepus.vplanapp;

/**
 * Created by samuel on 20.11.14.
 */
public class ExamsRow {

    private String date;
    private String subject;

    public ExamsRow(String date, String subject) {

        this.date = date;
        this.subject = subject;
    }

    public String getDate() {
        return date;
    }

    public String getSubject() {
        return subject;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
