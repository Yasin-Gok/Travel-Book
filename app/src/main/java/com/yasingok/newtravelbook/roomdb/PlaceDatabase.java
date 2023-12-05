package com.yasingok.newtravelbook.roomdb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.yasingok.newtravelbook.model.Place;

@Database(entities = {Place.class}, version = 1)
public abstract class PlaceDatabase extends RoomDatabase {
    public abstract PlaceDao placeDao();
}