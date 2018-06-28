package com.jrvermeer.psalter.Infrastructure.Expansion;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.os.Environment;
import android.widget.Toast;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.jrvermeer.psalter.Core.Models.Psalter;
import com.jrvermeer.psalter.R;
import com.jrvermeer.psalter.UI.Activities.MainActivity;

import java.io.InputStream;

/**
 * Created by Jonathan on 6/23/2018.
 */

public class ExpansionHelper {
    private static final int EXPANSION_MAIN = 20;
    private static final int EXPANSION_PATCH = 0;
    private Context context;

    public ExpansionHelper(Context context){
        this.context = context;
    }

    public Drawable getImage(boolean main, String path){
        ZipResourceFile folder = getZipResourceFile();
        if(folder == null) return null;

        try{
            String subFolder = (main ? "main." + EXPANSION_MAIN : "patch." + EXPANSION_PATCH)
                    + "." + context.getPackageName();
            if(!path.startsWith("/")) subFolder += "/";

            InputStream stream = folder.getInputStream(subFolder + path);
            return Drawable.createFromStream(stream, "src");
        }
        catch (Exception ex){
            Toast.makeText(context, "Error reading image", Toast.LENGTH_SHORT).show();
            return  null;        }
    }

    public AssetFileDescriptor getAudioDescriptor(boolean main, String path) {
        ZipResourceFile folder = getZipResourceFile();
        if(folder == null) return null;

        String subFolder = (main ? "main." + EXPANSION_MAIN : "patch." + EXPANSION_PATCH)
                + "." + context.getPackageName();
        if(!path.startsWith("/")) subFolder += "/";

        return folder.getAssetFileDescriptor(subFolder + path);
    }

    public boolean expansionFilesDownloaded(){
        return getZipResourceFile() != null;
    }

    private ZipResourceFile getZipResourceFile(){
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return APKExpansionSupport.getAPKExpansionZipFile(context, EXPANSION_MAIN, EXPANSION_PATCH);
            }
            else {
                Toast.makeText(context, "Phone storage must be mounted to play audio and view score", Toast.LENGTH_SHORT).show();
                return  null;
            }
        }
        catch (Exception ex){
            return  null;
        }
    }

}

