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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyActivity extends Activity implements View.OnClickListener {

    private TextView mTextView;
    private OverlayView mOverlayView;
    private MyOutputManager mMyOutputManager;
    private DismissOverlayView mDismissOverlayView;
    private GestureDetectorCompat mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        mMyOutputManager = new MyOutputManager(this);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                Logging.debug("onLayoutInflated for WatchViewStub");
                mTextView = (TextView)stub.findViewById(R.id.text);
                mOverlayView = (OverlayView)stub.findViewById(R.id.overlay);
                mDismissOverlayView = (DismissOverlayView)stub.findViewById(R.id.dismiss);
                mMyOutputManager.setTextView(mTextView);

                // Recursive add a listener for every View in the hierarchy, this is the only way to get all clicks
                ListenerHelper.recursiveSetOnClickListener(stub, MyActivity.this);

                // Prevent display from sleeping
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // Add a listener to handle closing the app on a long press to the activity
                mDismissOverlayView.setIntroText("Long press to exit");
                mDismissOverlayView.showIntroIfNecessary();
                mGestureDetector = new GestureDetectorCompat(MyActivity.this, new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public void onLongPress (MotionEvent e){
                        Logging.debug("Detected long press, showing exit overlay");
                        mDismissOverlayView.show();
                    }
                });

            }
        });

        stub.setOnApplyWindowInsetsListener(new WatchViewStub.OnApplyWindowInsetsListener() {
           @Override
           public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
               Logging.debug("onApplyWindowInsets for WatchViewStub, round=" + windowInsets.isRound());
               stub.onApplyWindowInsets(windowInsets);
               mMyOutputManager.handleWindowInsets(windowInsets);
               // WatchViewStub seems to call onApplyWindowInsets() multiple times before
               // the layout is inflated, so make sure we check the reference is valid.
               if (mOverlayView != null)
                   mOverlayView.setRound(windowInsets.isRound());
               return windowInsets;
           }
        });
    }

    @Override
    public void onClick (View v) {
        mMyOutputManager.nextView();
    }

    // Deliver touch events from the activity to the long press detector
    @Override
    public boolean dispatchTouchEvent (MotionEvent e) {
        return mGestureDetector.onTouchEvent(e) || super.dispatchTouchEvent(e);
    }
}
