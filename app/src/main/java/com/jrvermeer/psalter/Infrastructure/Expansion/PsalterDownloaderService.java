package com.jrvermeer.psalter.Infrastructure.Expansion;

import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.impl.DownloaderService;
import com.jrvermeer.psalter.R;

/**
 * Created by Jonathan on 6/23/2018.
 */

public class PsalterDownloaderService extends DownloaderService {

    public PsalterDownloaderService(){
        setDownloadFlags(IDownloaderService.FLAGS_DOWNLOAD_OVER_CELLULAR);
    }
    public static final byte[] SALT = new byte[] { 4, 46, 31, -122, -54, 62, 112, -76, -31, 9, -8, -34, 19, 25, -13, -117, -23, 43, -21, 24 };

    @Override
    public String getPublicKey() {
        return getString(R.string.secret_appkey);
    }

    @Override
    public byte[] getSALT() {
        return SALT;
    }

    @Override
    public String getAlarmReceiverClassName() {
        return PsalterAlarmReceiver.class.getName();
    }
}
