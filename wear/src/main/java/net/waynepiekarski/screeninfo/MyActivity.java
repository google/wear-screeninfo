package net.waynepiekarski.screeninfo;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
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
                mMyOutputManager.setTextView(mTextView);

                // Recursive add a listener for every View in the hierarchy, this is the only way to get all clicks
                ListenerHelper.recursiveSetOnClickListener(stub, MyActivity.this);

                // Prevent display from sleeping
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
}
