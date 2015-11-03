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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {

    MyOutputManager mMyOutputManager;
    Paint mPaintLines;
    Paint mPaintText;
    boolean mRound;
    int mCanvasWidth = -1;
    int mCanvasHeight = -1;
    int mViewWidth = -1;
    int mViewHeight = -1;

    public OverlayView (Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaintLines = new Paint ();
        mPaintLines.setColor(Color.RED);
        mPaintLines.setStyle(Paint.Style.STROKE);

        mPaintText = new Paint ();
        mPaintText.setColor(Color.WHITE);
        mPaintText.setAntiAlias(true);
        mPaintText.setTextSize(getResources().getDimensionPixelSize(R.dimen.small_text_size));
    }

    public void setRound (boolean in) {
        mRound = in;
        invalidate();
    }

    public void setMyOutputManager (MyOutputManager in) {
        mMyOutputManager = in;
    }

    @Override
    protected void onDraw (Canvas canvas) {
        // Refresh the output manager with the current Canvas and View dimensions
        if ((mCanvasWidth != canvas.getWidth())
                || (mCanvasHeight != canvas.getHeight())
                || (mViewWidth != this.getWidth())
                || (mViewHeight != this.getHeight())) {
            mCanvasWidth = canvas.getWidth();
            mCanvasHeight = canvas.getHeight();
            mViewWidth = this.getWidth();
            mViewHeight = this.getHeight();
            mMyOutputManager.handleCanvasViewSizes(canvas.getWidth(), canvas.getHeight(), this.getWidth(), this.getHeight());
        }

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
            canvas.drawLine(0, 0, canvas.getWidth()-1, 0, mPaintLines);
            canvas.drawLine(canvas.getWidth()-1, 0, canvas.getWidth()-1, canvas.getHeight()-1, mPaintLines);
            canvas.drawLine(canvas.getWidth()-1, canvas.getHeight()-1, 0, canvas.getHeight()-1, mPaintLines);
            canvas.drawLine(0, canvas.getHeight()-1, 0, 0, mPaintLines);
        }

        // Always print the View and Canvas (if it differs) dimensions at the top
        int textHeight = (int)(mPaintText.descent() - mPaintText.ascent());
        drawCenterText(canvas, getWidth() + "x" + getHeight(), textHeight, mPaintText);
        if ((canvas.getWidth() != getWidth()) || (canvas.getHeight() != getHeight()))
            drawCenterText(canvas, "Canvas=" + canvas.getWidth() + "x" + canvas.getHeight(), textHeight*2, mPaintText);
    }

    private void drawCenterText(Canvas c, String s, int y, Paint p) {
        c.drawText(s, (c.getWidth() - p.measureText(s))/2, y, mPaintText);
    }
}
