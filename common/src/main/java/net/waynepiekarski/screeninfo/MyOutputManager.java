package net.waynepiekarski.screeninfo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

public class MyOutputManager {

    private TextView mTextView;
    private Activity mActivity;
    private TextView mFixedBox20mm;
    private TextView mFixedBox1in;
    private TextView mFixedBox100dp;

    public MyOutputManager(Activity in) {
        mActivity = in;

        mBuild = ""
                + "ID=" + android.os.Build.ID + "\n"
                + "Display=" + android.os.Build.DISPLAY + "\n"
                + "Fingerprint=" + android.os.Build.FINGERPRINT + "\n"
                + "Tags=" + android.os.Build.TAGS + "\n"
                + "Type=" + android.os.Build.TYPE + "\n"
                + "User=" + android.os.Build.USER;
        Logging.debug ("Build string is:\n" + mBuild);

        mDevice = ""
                + "Board=" + android.os.Build.BOARD + "\n"
                + "Bootloader=" + android.os.Build.BOOTLOADER + "\n"
                + "Brand=" + android.os.Build.BRAND + "\n"
                + "ABI=" + android.os.Build.CPU_ABI + "\n"
                + "Device=" + android.os.Build.DEVICE + "\n"
                + "Hardware=" + android.os.Build.HARDWARE + "\n"
                + "Manuf=" + android.os.Build.MANUFACTURER + "\n"
                + "Model=" + android.os.Build.MODEL + "\n"
                + "Product=" + android.os.Build.PRODUCT;
        Logging.debug ("Device string is:\n" + mDevice);

        // ANDROID_ID should be unique across all devices according to:
        // http://android-developers.blogspot.co.uk/2011/03/identifying-app-installations.html
        mSerial = ""
                + "Host=" + android.os.Build.HOST + "\n"
                + "Radio=" + android.os.Build.getRadioVersion() + "\n"
                + "Serial=" + android.os.Build.SERIAL + "\n"
                + "AndroidId=" + Settings.Secure.getString(in.getContentResolver(), Settings.Secure.ANDROID_ID);
        Logging.debug ("Serial string is:\n" + mSerial);

        mAppInfo = ""
                + "Pkg=" + BuildConfig.APPLICATION_ID + "\n"
                + "Type=" + BuildConfig.BUILD_TYPE + "\n"
                + "VName=" + BuildConfig.VERSION_NAME + "\n"
                + "VCode=" + BuildConfig.VERSION_CODE + "\n"
                + "Flavor=" + BuildConfig.FLAVOR + "\n"
                + "Debug=" + BuildConfig.DEBUG;
        Logging.debug ("Application info string is:\n" + mAppInfo);

        // Calculate accurate values for the display dimensions
        // From CorayThan @ http://stackoverflow.com/questions/14341041/how-to-get-real-screen-height-and-width/23861333#23861333
        Display display = in.getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Logging.debug ("Could not use reflection to get the real display metrics");
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        // Query the metrics for other display information like DPI and density
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float inchX = realWidth/metrics.xdpi;
        float inchY = realHeight/metrics.ydpi;
        int dpX = (int)(realWidth / metrics.scaledDensity);
        int dpY = (int)(realHeight / metrics.scaledDensity);

        mDPI = "Display=" + realWidth + "x" + realHeight + " (pixels)\n"
                + "density=" + metrics.density + " (x160=" + (int)(metrics.density * 160) + "dpi)\n"
                + "densityDpi=" + metrics.densityDpi + "(" + convertDpiToString(metrics) + ")\n"
                + "scaledDensity=" + metrics.scaledDensity + "\n"
                + "xdpi=" + metrics.xdpi + "\n"
                + "ydpi=" + metrics.ydpi + "\n"
                + "inches=" + String.format("%.2f", inchX) + "\"x" + String.format("%.2f", inchY) + "\"" + "\n"
                + "dp=" + dpX + "x" + dpY;
        Logging.debug("DPI string is:\n" + mDPI);

        WifiManager wifiManager = (WifiManager)mActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String wifiAddress = wifiInfo.getMacAddress();
        if (wifiAddress == null)
            wifiAddress = "No Wifi";

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        String btAddress = "No Bluetooth";
        if (btAdapter != null)
          btAddress = btAdapter.getAddress();

        // Reconstitute the pairing device name from the model and the last 4 digits of the bluetooth MAC
        String wearName;
        if ((btAddress != null) && (!btAddress.equals("No Bluetooth"))) {
            wearName = android.os.Build.MODEL;
            String[] tokens = btAddress.split(":");
            wearName += " " + tokens[4] + tokens[5];
            wearName = wearName.toUpperCase();
        } else {
            wearName = "No Bluetooth";
        }

        mAddresses = "Pair=" + wearName + "\n"
                + "BT=" + btAddress + "\n"
                + "WiFi=" + wifiAddress;
        Logging.debug("Address string is:\n" + mAddresses);

        refreshView();
    }

    // Devices with no insets may or may not run our callback, so have a default ready here
    private String mWindowInsets = "No WindowInsets Returned\nDevice is square with no insets";
    private String mDPI = "n/a";
    private String mDevice = "n/a";
    private String mBuild = "n/a";
    private String mSerial = "n/a";
    private String mAppInfo = "n/a";
    private String mAddresses = "n/a";
    private String mFixedBox = "";
    private int mTextItem = 0;
    RelativeLayout mFixedBoxesView;

    public void setTextView (TextView in) {
        mTextView = in;
        // Grab a reference to the fixed boxes and calculate their dimensions since
        // we know the layout is now inflated.
        mFixedBoxesView = (RelativeLayout)mActivity.findViewById(R.id.fixed_boxes);
        mFixedBox20mm = (TextView)mActivity.findViewById(R.id.box_20mm);
        mFixedBox1in = (TextView)mActivity.findViewById(R.id.box_1in);
        mFixedBox100dp = (TextView)mActivity.findViewById(R.id.box_100dp);

        refreshView();
    }

    public void handleWindowInsets (WindowInsets windowInsets) {
        String shape = windowInsets.isRound() ? "Round" : "Square";

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mWindowInsets = "Display=" + metrics.widthPixels + "x" + metrics.heightPixels + "\n"
                + "Shape=" + shape + "\n"
                + "Insets L" + windowInsets.getSystemWindowInsetLeft() +
                ", R" + windowInsets.getSystemWindowInsetRight() + ", T" + windowInsets.getSystemWindowInsetTop() +
                ", B" + windowInsets.getSystemWindowInsetBottom();
        Logging.debug("Insets string is:\n" + mWindowInsets);
        refreshView();
    }

    static String convertDpiToString (DisplayMetrics metrics) {
        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_LOW: return "ldpi";
            case DisplayMetrics.DENSITY_MEDIUM: return "mdpi";
            case DisplayMetrics.DENSITY_HIGH: return "hdpi";
            case DisplayMetrics.DENSITY_TV: return "tv";
            case DisplayMetrics.DENSITY_XHIGH: return "xhdpi";
            case DisplayMetrics.DENSITY_XXHIGH: return "xxhdpi";
            case DisplayMetrics.DENSITY_XXXHIGH: return "xxxhdpi";
            case DisplayMetrics.DENSITY_400: return "400";
            default: return "unknown-" + metrics.densityDpi;
        }
    }

    public void visibleFixedBox(boolean active) {
        if (active)
            mFixedBoxesView.setVisibility(View.VISIBLE);
        else
            mFixedBoxesView.setVisibility(View.INVISIBLE);

        if (active) {
            mFixedBox = ""
                    + "1in=" + mFixedBox1in.getWidth() + "x" + mFixedBox1in.getHeight() + "\n"
                    + "20mm=" + mFixedBox20mm.getWidth() + "x" + mFixedBox20mm.getHeight() + "\n"
                    + "100dp=" + mFixedBox100dp.getWidth() + "x" + mFixedBox100dp.getHeight();
            Logging.debug("Fixed box string is:\n" + mFixedBox);
        }
    }

    // Change mNumPages whenever you adjust the switch() statement below
    private final int mNumPages = 8;

    public void refreshView() {
        if (mTextView == null) return; // Prevent crashes if window inset is called before layout inflate

        switch (mTextItem) {
            case 0: visibleFixedBox(false); mTextView.setText(mDPI); break;
            case 1: mTextView.setText(mWindowInsets); break;
            case 2: mTextView.setText(mAddresses); break;
            case 3: mTextView.setText(mDevice); break;
            case 4: mTextView.setText(mBuild); break;
            case 5: mTextView.setText(mSerial); break;
            case 6: mTextView.setText(mAppInfo); break;
            case 7: visibleFixedBox(true); mTextView.setText(mFixedBox); break;
            default: Logging.fatal("Unknown item " + mTextItem);
        }
    }

    public void nextView () {
        /* Cycle through all the strings when the user touches the TextView */
        mTextItem++;
        if (mTextItem >= mNumPages) mTextItem = 0;
        refreshView();
    }
}
