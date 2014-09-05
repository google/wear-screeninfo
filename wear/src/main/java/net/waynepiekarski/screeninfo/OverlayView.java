package net.waynepiekarski.screeninfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.wearable.view.WatchViewStub;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

public class OverlayView extends View {

    Paint mPaintLines;
    boolean mRound;

    public OverlayView (Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaintLines = new Paint ();
        mPaintLines.setColor(Color.RED);
        mPaintLines.setStyle(Paint.Style.STROKE);

        setOnApplyWindowInsetsListener(new WatchViewStub.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                OverlayView.this.onApplyWindowInsets(windowInsets);
                mRound = windowInsets.isRound();
                return windowInsets;
            }
        });
    }

    @Override
    protected void onDraw (Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.DKGRAY);
        canvas.drawLine(0, 0, canvas.getWidth(), canvas.getHeight(), mPaintLines);
        canvas.drawLine(0, canvas.getHeight(), canvas.getWidth(), 0, mPaintLines);
        canvas.drawLine(0, canvas.getHeight()/2, canvas.getWidth(), canvas.getHeight()/2, mPaintLines);
        canvas.drawLine(canvas.getWidth() / 2, 0, canvas.getWidth() / 2, canvas.getHeight(), mPaintLines);
        if (mRound) {
            // Use -2 pixels to make the circle visible on the display
            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, canvas.getWidth() / 2 - 2, mPaintLines);
        } else {
            // Draw a box outline for a rectangular display
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mPaintLines);
        }
    }
}
