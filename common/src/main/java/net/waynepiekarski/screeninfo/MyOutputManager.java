package net.waynepiekarski.screeninfo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyOutputManager {

    private TextView mTextView;
    private Activity mActivity;
    private TextView mFixedBox20mm;
    private TextView mFixedBox1in;

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

        mSerial = ""
                + "Host=" + android.os.Build.HOST + "\n"
                + "Radio=" + android.os.Build.getRadioVersion() + "\n"
                + "Serial=" + android.os.Build.SERIAL;
        Logging.debug ("Serial string is:\n" + mSerial);

        mAppInfo = ""
                + "Pkg=" + BuildConfig.PACKAGE_NAME + "\n"
                + "Type=" + BuildConfig.BUILD_TYPE + "\n"
                + "VName=" + BuildConfig.VERSION_NAME + "\n"
                + "VCode=" + BuildConfig.VERSION_CODE + "\n"
                + "Flavor=" + BuildConfig.FLAVOR + "\n"
                + "Debug=" + BuildConfig.DEBUG;
        Logging.debug ("Application info string is:\n" + mAppInfo);

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float inchX = metrics.widthPixels/metrics.xdpi;
        float inchY = metrics.heightPixels/metrics.ydpi;

        mDPI = "Display=" + metrics.widthPixels + "x" + metrics.heightPixels + "\n"
                + "density=" + metrics.density + "\n"
                + "densityDpi=" + metrics.densityDpi + "(" + convertDpiToString(metrics) + ")\n"
                + "scaledDensity=" + metrics.scaledDensity + "\n"
                + "xdpi=" + metrics.xdpi + "\n"
                + "ydpi=" + metrics.ydpi + "\n"
                + "inches=" + String.format("%.2f", inchX) + "\"x" + String.format("%.2f", inchY) + "\"";
        Logging.debug("DPI string is:\n" + mDPI);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        WifiManager wifiManager = (WifiManager)mActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String btAddress = btAdapter.getAddress();
        String wifiAddress = wifiInfo.getMacAddress();
        if (wifiAddress == null)
            wifiAddress = "No Wifi";

        // Reconstitute the pairing device name from the model and the last 4 digits of the bluetooth MAC
        String wearName;
        if (btAddress != null) {
            wearName = android.os.Build.MODEL;
            String[] tokens = btAddress.split(":");
            wearName += " " + tokens[4] + tokens[5];
            wearName = wearName.toUpperCase();
        } else {
            wearName = "No Bluetooth";
        }

        mAddresses = "Pair=" + wearName + "\n"
                + "BT=" + btAdapter.getAddress() + "\n"
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
                    + "20mm=" + mFixedBox20mm.getWidth() + "x" + mFixedBox20mm.getHeight() + "\n"
                    + "1in=" + mFixedBox1in.getWidth() + "x" + mFixedBox1in.getHeight();
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
