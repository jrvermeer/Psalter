package com.vermeer.jonathan.psalter;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Created by Jonathan on 4/19/2017.
 */

public class LoadDatabaseTask extends AsyncTask<Context, Void, Void> {
    private Context _context;
    private ProgressDialog _progressDialog;
    private PsalterDb _db;
    private SQLiteDatabase _sqlDb;
    public LoadDatabaseTask(Context context, PsalterDb db) {
        super();
        _context = context;
        _db = db;
    }

    @Override
    protected Void doInBackground(Context... contexts) {
        try{
            _sqlDb = _db.getReadableDatabase();
        }
        catch (Exception ex){
            Toast.makeText(_context, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        _progressDialog = new ProgressDialog(_context);
        _progressDialog.setMessage("Loading Psalter...");
        _progressDialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        _progressDialog.dismiss();
    }

    public SQLiteDatabase getSQLiteDatabase(){
        return _sqlDb;
    }
}
