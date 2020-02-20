package com.android.vending.expansion.downloader;

public class R {
  public class string {
    /*  When a download completes, a notification is displayed, and this
        string is used to indicate that the download successfully completed.
        Note that such a download could have been initiated by a variety of
        applications, including (but not limited to) the browser, an email
        application, a content marketplace. */
    static public final String notification_download_complete = "Download complete";

    /*  When a download completes, a notification is displayed, and this
        string is used to indicate that the download failed.
        Note that such a download could have been initiated by a variety of
        applications, including (but not limited to) the browser, an email
        application, a content marketplace. */
    static public final String notification_download_failed = "Download unsuccessful";


    static public final String state_unknown = "Starting...";
    static public final String state_idle = "Waiting for download to start";
    static public final String state_fetching_url = "Looking for resources to download";
    static public final String state_connecting = "Connecting to the download server";
    static public final String state_downloading = "Downloading resources";
    static public final String state_completed = "Download finished";
    static public final String state_paused_network_unavailable = "Download paused because no network is available";
    static public final String state_paused_network_setup_failure = "Download paused. Test a website in browser";
    static public final String state_paused_by_request = "Download paused";
    static public final String state_paused_wifi_unavailable = "Download paused because wifi is unavailable";
    static public final String state_paused_wifi_disabled = "Download paused because wifi is disabled";
    static public final String state_paused_roaming = "Download paused because you are roaming";
    static public final String state_paused_sdcard_unavailable = "Download paused because the external storage is unavailable";
    static public final String state_failed_unlicensed = "Download failed because you may not have purchased this app";
    static public final String state_failed_fetching_url = "Download failed because the resources could not be found";
    static public final String state_failed_sdcard_full = "Download failed because the external storage is full";
    static public final String state_failed_cancelled = "Download cancelled";
    static public final String state_failed = "Download failed";

    static public final String kilobytes_per_second = "%1$s KB/s";
    static public final String time_remaining = "Time remaining: %1$s";
    static public final String time_remaining_notification = "%1$s left";
  }
}
