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
import com.google.android.vending.expansion.zipfile.ZipResourceFile;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.InputStream;
import java.io.IOException;

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

    class CallbackMessage {
        static final int TYPE_DOWNLOAD_STATE_CHANGED = 0;
        static final int TYPE_DOWNLOAD_PROGRESS = 1;
        int type;
        int state;
        DownloadProgressInfo progress;
    }
    ConcurrentLinkedQueue<CallbackMessage> messageQueue = new ConcurrentLinkedQueue<CallbackMessage>();

    static native void onDownloadProgressNative(DownloadProgressInfo progress);
    static native void onDownloadStateChangedNative(int newState);

    @Override
    public void onDownloadStateChanged(int newState) {
        CallbackMessage message = new CallbackMessage();
        message.type = CallbackMessage.TYPE_DOWNLOAD_STATE_CHANGED;
        message.state = newState;
        messageQueue.add(message);
    }

    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        CallbackMessage message = new CallbackMessage();
        message.type = CallbackMessage.TYPE_DOWNLOAD_PROGRESS;
        message.progress = progress;
        messageQueue.add(message);
    }

    public static final void flushMessageQueue() {
        DefoldInterface this_ = DefoldInterface.getSingleton();
        while (true) {
            CallbackMessage message = this_.messageQueue.poll();
            if (message == null) { return; }
            if (message.type == CallbackMessage.TYPE_DOWNLOAD_STATE_CHANGED) {
                DefoldInterface.onDownloadStateChangedNative(message.state);
            } else if (message.type == CallbackMessage.TYPE_DOWNLOAD_PROGRESS) {
                DefoldInterface.onDownloadProgressNative(message.progress);
            }
        }
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

            int result = DownloaderClientMarshaller.startDownloadServiceIfRequired(activity,
                PendingIntent.getActivity(activity, 0, intentToLaunchThisActivityFromNotification, PendingIntent.FLAG_UPDATE_CURRENT),
                APKXDownloaderService.class
            );

            if (result != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                final DefoldInterface _this = this;
                final Activity _activity = activity;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(_this, APKXDownloaderService.class);
                        mDownloaderClientStub.connect(_activity);
                    }
                });
                return true;
            }
        } catch (NameNotFoundException e) {
            Log.e(LOG_TAG, "Cannot find own package! MAYDAY!");
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error happened!");
            e.printStackTrace();
        }
        return false;
    }

    final void _onActivateApp(Activity activity) {
        final Activity _activity = activity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != mDownloaderClientStub) {
                    mDownloaderClientStub.connect(_activity);
                }
            }
        });
    }

    final void _onDeactivateApp(Activity activity) {
        final Activity _activity = activity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != mDownloaderClientStub) {
                    mDownloaderClientStub.disconnect(_activity);
                }
            }
        });
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

    public static final void onActivateApp(Activity activity) {
        DefoldInterface.getSingleton()._onActivateApp(activity);
    }

    public static final void onDeactivateApp(Activity activity) {
        DefoldInterface.getSingleton()._onDeactivateApp(activity);
    }

    public static final byte[] zipGetFile(ZipResourceFile zip, String path) throws IOException {
        InputStream stream = zip.getInputStream(path);
        if (stream == null) {
            return null;
        }

        int uncompressedLength = (int)zip.getUncompressedLength(path);
        byte[] bytes = new byte[uncompressedLength];

        long readBytes = stream.read(bytes, 0, uncompressedLength);
        if (readBytes < uncompressedLength) {
            return null;
        }

        return bytes;
    }
}