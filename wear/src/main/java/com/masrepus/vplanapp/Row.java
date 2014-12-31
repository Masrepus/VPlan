package com.masrepus.vplanapp;

import java.io.Serializable;

/**
 * Used to bundle class, lesson and status of a specific list item
 */
public class Row implements Serializable {
    private long id;
    private String klasse;
    private String stunde;
    private String status;

    public Row(String stunde, String klasse, String status) {
        this.klasse = klasse;
        this.stunde = stunde;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKlasse() {
        return klasse;
    }

    public void setKlasse(String klasse) {
        this.klasse = klasse;
    }

    public String getStunde() {
        return stunde;
    }

    public void setStunde(String stunde) {
        this.stunde = stunde;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 