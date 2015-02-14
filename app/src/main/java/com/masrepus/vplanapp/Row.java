package com.masrepus.vplanapp;

import com.google.android.gms.wearable.DataMap;

import java.io.Serializable;

/**
 * Used to bundle class, lesson and status of a specific list item
 */
public class Row implements Serializable {
    private long id;
    private String klasse;
    private String stunde;
    private String status;


    public Row() {
    }

    public Row(DataMap map) {
        stunde = map.getString("stunde");
        status = map.getString("status");
        klasse = map.getString("klasse");
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

    public DataMap putToDataMap(DataMap map) {
        map.putString("stunde", stunde);
        map.putString("klasse", klasse);
        map.putString("status", status);
        return map;
    }
} 