package com.masrepus.vplanapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by samuel on 20.11.14.
 */
public class ExamsRow {

    private String dateString;
    private String subject;
    private String type;
    private String grade;

    public ExamsRow(String date, String grade, String subject, String type) {

        this.dateString = date;
        this.subject = subject;
        this.type = type;
        this.grade = grade;
    }

    public String getDateString() {
        return dateString;
    }

    public Date getDate() {

        Date date;
        try {
            date = new SimpleDateFormat("dd.MM.yyyy").parse(dateString);
        } catch (ParseException e) {
            date = null;
        }

        return date;
    }

    public String getSubject() {
        return subject;
    }

    public String getType() {
        return type;
    }

    public String getGrade() {
        return grade;
    }
}
