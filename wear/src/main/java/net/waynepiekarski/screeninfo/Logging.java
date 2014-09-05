package net.waynepiekarski.screeninfo;

import android.util.Log;

public class Logging {

    private static final String TAG = "ScreenInfo";

    public static void debug (String str) {
        Log.d (TAG, str);
    }

    public static void fatal (String str) {
        Log.e (TAG, "FATAL ERROR: " + str);
        RuntimeException re = new RuntimeException();
        re.printStackTrace();
        Log.e (TAG, "Exiting with error code 1");
        System.exit(1);
    }

}
