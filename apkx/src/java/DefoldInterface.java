package me.petcu.defoldapkx;

import android.content.Context;
import android.app.Activity;
import com.google.android.vending.expansion.downloader.Constants;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;

// import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
// import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
// import android.net.Uri;
// import android.os.AsyncTask;
// import android.os.Build;
// import android.os.Bundle;
import android.os.Messenger;
// import android.os.SystemClock;
// import android.provider.Settings;
// import android.support.design.widget.Snackbar;
// import android.support.v4.app.ActivityCompat;
// import android.support.v7.app.AppCompatActivity;
import android.util.Log;
// import android.view.View;
// import android.widget.Button;
// import android.widget.ProgressBar;
// import android.widget.TextView;

class DefoldInterface implements IDownloaderClient {
    private static final String LOG_TAG = "LVLDownloader";

    public static final String getExpansionAPKFilePath(Context context, boolean mainFile, int versionCode) {
        return Helpers.generateSaveFileName(context,
            Helpers.getExpansionAPKFileName(context, mainFile, versionCode)
        );
    }

    public static String publicKey;
    public static byte[] salt;

    public static final void configureDownloadService(Context context, String _publicKey, byte[] _salt) {
        DefoldInterface.publicKey = _publicKey;
        DefoldInterface.salt = _salt;
    }

    private IDownloaderService mRemoteService;
    private IStub mDownloaderClientStub;

    @Override
    public void onServiceConnected(Messenger m) {
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }

    @Override
    public void onDownloadStateChanged(int newState) {
        Log.i(LOG_TAG, "onDownloadStateChanged");
    }

    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        Log.i(LOG_TAG, "onDownloadProgress");
    }

    final boolean _startDownloadServiceIfRequired(Activity activity) {
        try {
            Intent launchIntent = activity.getIntent();
            Intent intentToLaunchThisActivityFromNotification = new Intent(activity, activity.getClass());
            intentToLaunchThisActivityFromNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intentToLaunchThisActivityFromNotification.setAction(launchIntent.getAction());

            if (launchIntent.getCategories() != null) {
                for (String category : launchIntent.getCategories()) {
                    intentToLaunchThisActivityFromNotification.addCategory(category);
                }
            }

            if (DownloaderClientMarshaller.startDownloadServiceIfRequired(activity,
                    PendingIntent.getActivity(activity, 0, intentToLaunchThisActivityFromNotification, PendingIntent.FLAG_UPDATE_CURRENT),
                    APKXDownloaderService.class
                ) != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED
            ) {
                mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(this, APKXDownloaderService.class);
                mDownloaderClientStub.connect(activity);
                return true;
            }
        } catch (NameNotFoundException e) {
            Log.e(LOG_TAG, "Cannot find own package! MAYDAY!");
            e.printStackTrace();
        }
        return false;
    }

    final void _onActivateApp(Context context) {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.connect(context);
        }
    }

    final void _onDeactivateApp(Context context) {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.disconnect(context);
        }
    }

    static DefoldInterface _singleton;
    public static final DefoldInterface getSingleton() {
        if (_singleton == null) {
            _singleton = new DefoldInterface();
        }
        return _singleton;
    }

    public static final boolean startDownloadServiceIfRequired(Activity activity) {
        return DefoldInterface.getSingleton()._startDownloadServiceIfRequired(activity);
    }

    public static final void onActivateApp(Context context) {
        DefoldInterface.getSingleton()._onActivateApp(context);
    }

    public static final void onDeactivateApp(Context context) {
        DefoldInterface.getSingleton()._onDeactivateApp(context);
    }
}