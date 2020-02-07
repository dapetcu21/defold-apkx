package com.defold.apkx;

import android.os.Vibrator;
import android.content.Context;

class NativeExample {
    public static final void vibratePhone(Context context, int vibrateMilliSeconds) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(vibrateMilliSeconds);
    }

    public static String DoStuff() {
        return "Message From Java!";
    }
}