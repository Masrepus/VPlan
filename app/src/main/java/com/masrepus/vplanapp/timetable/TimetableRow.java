package com.masrepus.vplanapp.timetable;

import java.io.Serializable;

/**
 * Created by samuel on 31.01.15.
 */
public class TimetableRow implements Serializable {

    private String lesson;
    private String subject;
    private String room;

    public TimetableRow(String lesson, String subject, String room) {
        this.lesson = lesson;
        this.subject = subject;
        this.room = room;
    }

    public String getLesson() {
        return lesson;
    }

    public String getRoom() {
        return room;
    }

    public String getSubject() {
        return subject;
    }
}
