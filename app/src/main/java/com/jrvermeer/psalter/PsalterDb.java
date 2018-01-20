package com.jrvermeer.psalter;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.jrvermeer.psalter.Adapters.PsalterSearchAdapter;
import com.jrvermeer.psalter.Models.Psalter;
import com.jrvermeer.psalter.Models.SqLiteQuery;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;

/**
 * Created by Jonathan on 3/27/2017.
 */
// SQLiteAssetHelper: https://github.com/jgilfelt/android-sqlite-asset-helper
public class PsalterDb extends SQLiteAssetHelper {
    private static final int DATABASE_VERSION = 12;
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
        } catch (Exception ex){;
            return 0;
        }
    }

    public Psalter getPsalter(int number){
        Cursor c = null;
        try{
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

            String[] columns = {"_id", "psalm", "lyrics", "numverses", "heading"};
            String where = "_id = " + number;
            qb.setTables(TABLE_NAME);
            c = qb.query(db, columns, where, null, null, null, null);
            if(c.moveToNext()){
                Psalter p = new Psalter();
                p.setNumber(c.getInt(0));
                p.setPsalm(c.getInt(1));
                p.setLyrics(c.getString(2));
                p.setNumverses(c.getInt(3));
                p.setHeading(c.getString(4));
                return p;
            } else {
                return null;
            }
        } catch (Exception ex){
            return null;
        }
        finally {
            if(c != null) c.close();
        }
    }

    public Psalter[] searchPsalter(String searchText){
        Cursor c = null;
        try{
            ArrayList<Psalter> hits = new ArrayList<>();
            c = searchPsalterCursor(searchText);
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
        finally {
            if(c != null) c.close();
        }
    }

    private Cursor searchPsalterCursor(String searchText){
        SqLiteQuery lyrics = LyricsReplacePunctuation();
        lyrics.addParameter("%" + searchText + "%");

        return db.rawQuery("select _id, psalm, " + lyrics.getQueryText() + " l from psalter where l like ?", lyrics.getParameters() );
    }

    private SqLiteQuery LyricsReplacePunctuation(){
        SqLiteQuery query = new SqLiteQuery();
        query.setQueryText("lyrics");
        for(int i = 0; i < PsalterSearchAdapter.ignoreChars.length; i++){
            query.setQueryText("replace(" + query.getQueryText() + ", ?, '')");
            query.addParameter(String.valueOf(PsalterSearchAdapter.ignoreChars[i]));
        }
        return query;
    }

    public Psalter[] getPsalm(int psalmNumber){
        Cursor c = null;
        try{
            ArrayList<Psalter> hits = new ArrayList<>();
            c = db.rawQuery("select _id, psalm, lyrics from psalter where psalm = " + String.valueOf(psalmNumber), null);
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
        finally {
            if(c != null) c.close();
        }
    }
}
