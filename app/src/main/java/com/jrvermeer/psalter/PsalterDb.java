package com.jrvermeer.psalter;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Jonathan on 3/27/2017.
 */
// SQLiteAssetHelper: https://github.com/jgilfelt/android-sqlite-asset-helper
public class PsalterDb extends SQLiteAssetHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "psalter.sqlite";
    private static  final String TABLE_NAME = "psalter";
    private SQLiteDatabase db;

    public PsalterDb(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade(DATABASE_VERSION);
        db = getReadableDatabase();
    }


    public int getCount(){
        try{
            return (int)DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        } catch (Exception ex){
            return 0;
        }
    }

    public Psalter getPsalter(int number){
        try{
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

            String[] columns = {"_id", "psalm", "lyrics"};
            String where = "_id = " + number;
            qb.setTables(TABLE_NAME);
            Cursor c = qb.query(db, columns, where, null, null, null, null);
            if(c.moveToNext()){
                Psalter p = new Psalter();
                p.setNumber(c.getInt(0));
                p.setPsalm(c.getInt(1));
                p.setLyrics(c.getString(2));
                return p;
            } else {
                return null;
            }
        } catch (Exception ex){
            return null;
        }
    }

    public Psalter[] searchPsalter(String searchText){
        try{
            ArrayList<Psalter> hits = new ArrayList<>();
            String lyrics = LyricsReplacePunctuation();
            Cursor c = db.rawQuery("select _id, psalm, " + lyrics + " from psalter where " + lyrics + " like '%" + searchText + "%'", null);
            while(c.moveToNext()){
                Psalter p = new Psalter();
                p.setNumber(c.getInt(0));
                p.setPsalm(c.getInt(1));
                p.setLyrics(c.getString(2));

                hits.add(p);
            }
            return hits.toArray(new Psalter[hits.size()]);
        } catch (Exception ex){
            return null;
        }
    }

    public String LyricsReplacePunctuation(){
        String replace = "lyrics";
        Collections.addAll(Arrays.asList(PsalterSearchAdapter.searchIgnoreChars));
        for(char replaceChar : PsalterSearchAdapter.searchIgnoreChars){
            replace = "replace(" + replace + ", char(" + (int)replaceChar + "), '')";
        }
        return replace;
    }

    public Psalter[] getPsalm(int psalmNumber){
        try{
            ArrayList<Psalter> hits = new ArrayList<>();
            Cursor c = db.rawQuery("select _id, psalm, lyrics from psalter where psalm = " + String.valueOf(psalmNumber), null);
            while(c.moveToNext()){
                Psalter p = new Psalter();
                p.setNumber(c.getInt(0));
                p.setPsalm(c.getInt(1));
                p.setLyrics(c.getString(2));

                hits.add(p);
            }
            return hits.toArray(new Psalter[hits.size()]);
        } catch (Exception ex){
            return null;
        }
    }
}
