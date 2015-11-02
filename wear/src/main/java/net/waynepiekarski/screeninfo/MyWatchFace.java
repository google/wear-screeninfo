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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mOverlayPaint;
        Paint mHandPaint;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        // Store all the information we know about the display
        boolean mLowBitAmbient;
        boolean mBurnInProtection;
        WindowInsets mWindowInsets = null;
        boolean mRound;
        int mSurfaceWidth;
        int mSurfaceHeight;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            mHandPaint = new Paint();
            mHandPaint.setColor(Color.LTGRAY);
            mHandPaint.setStrokeWidth(getResources().getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mTextPaint = new Paint();
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size));

            mOverlayPaint = new Paint();
            mOverlayPaint.setColor(Color.WHITE);
            mOverlayPaint.setStyle(Paint.Style.STROKE);

            mTime = new Time();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mRound = insets.isRound();
            mWindowInsets = insets;
            Logging.debug("onApplyWindowInsets(" + insets + ")");
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            Logging.debug("onPropertiesChanged(PROPERTY_LOW_BIT_AMBIENT)=" + mLowBitAmbient);
            Logging.debug("onPropertiesChanged(PROPERTY_BURN_IN_PROTECTION)=" + mBurnInProtection);
            recalculatePaint();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            Logging.debug("onSurfaceChanged(width=" + width + ", height=" + height + ")");
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        private void recalculatePaint() {
            if (isInAmbientMode() && !mLowBitAmbient && !mBurnInProtection) {
                mOverlayPaint.setColor(Color.DKGRAY);
            } else {
                mOverlayPaint.setColor(Color.RED);
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                recalculatePaint();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;


            if (!mAmbient) {
                float secOuter = centerX - centerX*0.05f;
                float secInner = centerX - centerX*0.1f;
                float secXo = (float) Math.sin(secRot) * secOuter;
                float secYo = (float) -Math.cos(secRot) * secOuter;
                float secXi = (float) Math.sin(secRot) * secInner;
                float secYi = (float) -Math.cos(secRot) * secInner;
                canvas.drawLine(centerX + secXi, centerY + secYi, centerX + secXo, centerY + secYo, mHandPaint);
            } else {
                float minLength = centerX - centerX * 0.25f;
                float hrLength = centerX - centerX * 0.5f;

                float minX = (float) Math.sin(minRot) * minLength;
                float minY = (float) -Math.cos(minRot) * minLength;
                canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaint);

                float hrX = (float) Math.sin(hrRot) * hrLength;
                float hrY = (float) -Math.cos(hrRot) * hrLength;
                canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaint);
            }

            // Draw a circle and crosses over the watch face so we can visualize the display shape
            canvas.drawLine(0, 0, bounds.width(), bounds.height(), mOverlayPaint);
            canvas.drawLine(0, bounds.height(), bounds.width(), 0, mOverlayPaint);
            canvas.drawLine(0, bounds.height()/2, bounds.width(), bounds.height()/2, mOverlayPaint);
            canvas.drawLine(bounds.width() / 2, 0, bounds.width() / 2, bounds.height(), mOverlayPaint);
            if (mRound) {
                // Use -2 pixels to make the circle visible on the display
                canvas.drawCircle(bounds.width() / 2, bounds.height() / 2, bounds.width() / 2 - 2, mOverlayPaint);
            } else {
                // Draw a box outline for a rectangular display
                canvas.drawLine(0, 0, bounds.width()-1, 0, mOverlayPaint);
                canvas.drawLine(bounds.width()-1, 0, bounds.width()-1, bounds.height()-1, mOverlayPaint);
                canvas.drawLine(bounds.width()-1, bounds.height()-1, 0, bounds.height()-1, mOverlayPaint);
                canvas.drawLine(0, bounds.height()-1, 0, 0, mOverlayPaint);
            }

            // Draw debugging text over the top when in active mode. Start at the top-left of a round
            // watch to ensure that it fits on all watch types easily.
            if (!mAmbient) {
                int rowHeight = (int) (mTextPaint.descent() - mTextPaint.ascent());
                int textX = (int) (centerX * (1.0d - Math.sin(Math.toRadians(45.0d))));
                int textY = (int) (centerY * (1.0d - Math.sin(Math.toRadians(45.0d)))) + rowHeight;

                if (mWindowInsets == null) {
                    canvas.drawText("shape=Square", textX, textY, mTextPaint);
                    textY += rowHeight;
                    canvas.drawText("insets=No insets received", textX, textY, mTextPaint);
                } else {
                    canvas.drawText("shape=" + (mWindowInsets.isRound() ? "Round" : "Square"), textX, textY, mTextPaint);
                    textY += rowHeight;
                    canvas.drawText("insets=L" + mWindowInsets.getSystemWindowInsetLeft() +
                            ", R" + mWindowInsets.getSystemWindowInsetRight() + ", T" + mWindowInsets.getSystemWindowInsetTop() +
                            ", B" + mWindowInsets.getSystemWindowInsetBottom(), textX, textY, mTextPaint);
                }
                textY += rowHeight;
                canvas.drawText("bounds=" + bounds.width() + "," + bounds.height(), textX, textY, mTextPaint);
                textY += rowHeight;
                canvas.drawText("canvas=" + canvas.getWidth() + "," + canvas.getHeight(), textX, textY, mTextPaint);
                textY += rowHeight;
                canvas.drawText("surface=" + mSurfaceWidth + "," + mSurfaceHeight, textX, textY, mTextPaint);
                textY += rowHeight;
                canvas.drawText("low_bit_ambient=" + mLowBitAmbient, textX, textY, mTextPaint);
                textY += rowHeight;
                canvas.drawText("burn_in_protection=" + mBurnInProtection, textX, textY, mTextPaint);
                textY += rowHeight;
                canvas.drawText("model=" + android.os.Build.MODEL, textX, textY, mTextPaint);
                textY += rowHeight;
                canvas.drawText("device=" + android.os.Build.DEVICE, textX, textY, mTextPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
