package com.yasingok.newtravelbook.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity
public class Place implements Serializable {                // Room kütüphanesi ile database oluşturuyoruz

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "longitude")
    public Double longitude;

    @ColumnInfo(name = "latitude")
    public Double latitude;

    public Place(String name, String description, Double longitude, Double latitude) {
        this.name = name;
        this.description = description;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}