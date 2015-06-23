// ---------------------------------------------------------------------
// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ---------------------------------------------------------------------

package net.waynepiekarski.screeninfo;

import net.waynepiekarski.screeninfo.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity implements View.OnClickListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;


    /**
     * Views necessary for the screen info app
     */
    private TextView mTextView;
    MyOutputManager mMyOutputManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final ViewGroup contentView = (ViewGroup)findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });


        /* Implement the interface for my app here */
        mMyOutputManager = new MyOutputManager(this);
        mTextView = (TextView)findViewById(R.id.text);
        mMyOutputManager.setTextView(mTextView);

        // Recursive add a listener for every View in the hierarchy, this is the only way to get all clicks
        ListenerHelper.recursiveSetOnClickListener(contentView, FullscreenActivity.this);

        // Prevent display from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Detect if we are running on Android TV
        UiModeManager uiModeManager = (UiModeManager)getSystemService(UI_MODE_SERVICE);
        boolean isTV;
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
            isTV = true;
        else
            isTV = false;

        // Allow the 'next' control at the bottom to advance to the next item.
        // Hide this control completely if we are on a TV device
        View nextButton = (View)findViewById(R.id.next_button);
        View wearButton = (View)findViewById(R.id.wear_button);
        if (isTV) {
            Logging.debug ("Detected Android TV, so hiding some non-TV views");
            nextButton.setVisibility(View.GONE);
            wearButton.setVisibility(View.GONE);
        } else {
            Logging.debug("Detected regular Android device with touchscreen, enabling 'next' and 'wear' view");
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMyOutputManager.nextView();
                }
            });
            wearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Must run this all on a background thread since it uses blocking calls for compactness
                    Logging.debug("onClick for wearable start button");
                    new Thread( new Runnable() {
                        @Override
                        public void run() {
                            Logging.debug("Connecting to Google Play Services to use MessageApi");
                            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(FullscreenActivity.this).addApi(Wearable.API).build();
                            ConnectionResult result = googleApiClient.blockingConnect();
                            if (result.isSuccess()) {
                                Logging.debug("Searching for list of wearable clients");
                                NodeApi.GetConnectedNodesResult nodesResult =
                                        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                                for (final Node node : nodesResult.getNodes()) {
                                    Logging.debug("Launching wearable client " + node.getId() + " via message");
                                    MessageApi.SendMessageResult sendResult =
                                            Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), "/start-on-wearable", new byte[0]).await();
                                    if (sendResult.getStatus().isSuccess()) {
                                        Logging.debug("Successfully sent to client " + node.getId());
                                    } else {
                                        Logging.debug("Failed to send to client " + node.getId() + " with error " + sendResult);
                                    }
                                }
                            } else {
                                Logging.debug("Failed to connect to Google Play Services: " + result);
                            }
                        }
                    }).start();
                }
            });
        }

        // Permanently hide the action bar
        getActionBar().hide();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_C:
                Logging.debug("onKeyDown: going to next view from keyCode=" + keyCode);
                mMyOutputManager.nextView();
                return true; // We processed the event
            default:
                Logging.debug("onKeyDown: unknown keyCode=" + keyCode);
                return false;
        }
    }

    @Override
    public void onClick (View v) {
        // Advance to the next view
        mMyOutputManager.nextView();

        /**
         * Touch listener to use for in-layout UI controls to delay hiding the
         * system UI. This is to prevent the jarring behavior of controls going away
         * while interacting with activity UI.
         */
        if (AUTO_HIDE)
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
