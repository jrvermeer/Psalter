package com.vermeer.jonathan.psalter;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Jonathan on 3/27/2017.
 */

public class PsalterDb extends SQLiteAssetHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "psalter.sqlite";
    private static  final String TABLE_NAME = "psalter";
    private SQLiteDatabase db;
    private IDatabaseCopyComplete iDatabaseCopyComplete;

    public PsalterDb(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        iDatabaseCopyComplete = (IDatabaseCopyComplete)context;
        if(!context.getDatabasePath(DATABASE_NAME).exists()){
            new AsyncTask<Void, Void, Void>(){
                public ProgressDialog _progress;
                SQLiteDatabase _sqlDb;
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    _progress = new ProgressDialog(context);
                    _progress.setMessage("Loading Psalter...");
                    _progress.show();
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    _sqlDb = PsalterDb.this.getReadableDatabase();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    _progress.dismiss();
                    db = _sqlDb;
                    iDatabaseCopyComplete.databaseCopyComplete();
                }

            }.execute();
        }
        else {
            db = getReadableDatabase();
        }
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

//    public byte[] getTune(int number){
//        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
//
//        String[] columns = {"tune"};
//        String where = "_id = " + number;
//        qb.setTables(TABLE_NAME);
//        Cursor c = qb.query(db, columns, where, null, null, null, null);
//        if(c.moveToNext()){
//            try{
//                return c.getBlob(0);
//            } catch (IllegalStateException ex){
//                Log.e("MainActivity", "audio too large to retrieve from database for #" + number);
//                return null;
//            }
//
//        }
//        else return null;
//    }

    public Psalter[] searchPsalter(String searchText){
        try{
            ArrayList<Psalter> hits = new ArrayList<>();
            String lyrics = LyricsReplacePunctuation();
            Cursor c = db.rawQuery("select _id, psalm, " + lyrics + " from psalter where " + lyrics + " like '%" + searchText + "%'", null);
            //Cursor c = db.rawQuery("select _id, psalm, lyrics from psalter where lyrics like '%" + searchText + "%'", null);
            while(c.moveToNext()){
                Psalter p = new Psalter();
                p.setNumber(c.getInt(0));
                p.setPsalm(c.getInt(1));
                p.setLyrics(c.getString(2));

                hits.add(p);
            }
            Psalter[] rtn = new Psalter[hits.size()];
            return hits.toArray(rtn);
        } catch (Exception ex){
            return null;
        }
    }

    public String LyricsReplacePunctuation(){
        String replace = "lyrics";
        ArrayList<String> replacements = new ArrayList<>();
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
            //Psalter[] rtn = new Psalter[hits.size()];
            return hits.toArray(new Psalter[hits.size()]);
        } catch (Exception ex){
            return null;
        }
    }

    public interface IDatabaseCopyComplete {
        void databaseCopyComplete();
    }
}
