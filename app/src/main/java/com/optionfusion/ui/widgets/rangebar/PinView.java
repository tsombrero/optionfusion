/*
 * Copyright 2014, Appyvet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.optionfusion.ui.widgets.rangebar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import com.optionfusion.R;

/**
 * Represents a thumb in the RangeBar slider. This is the handle for the slider
 * that is pressed and slid.
 */
class PinView extends View {

    // Private Constants ///////////////////////////////////////////////////////

    // The radius (in dp) of the touchable area around the thumb. We are basing
    // this value off of the recommended 48dp Rhythm. See:
    // http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
    private static final float MINIMUM_TARGET_RADIUS_DP = 24;

    private static final float DEFAULT_THUMB_WIDTH_DP = 28;
    private static final float DEFAULT_THUMB_HEIGHT_DP = 14;

    // Member Variables ////////////////////////////////////////////////////////

    // Radius (in pixels) of the touch area of the thumb.
    private float mTargetRadiusPx;

    // Indicates whether this thumb is currently pressed and active.
    private boolean mIsPressed = false;

    // The y-position of the thumb in the parent view. This should not change.
    private float mY;

    // The current x-position of the thumb in the parent view.
    private float mX;

    // mPaint to draw the thumbs if attributes are selected
    private Paint mTextPaint;

    private Drawable mPin;

    private String mValue;

    // Size of the thumb if selected
    private int mPinWidthPx = 80;
    private int mPinHeightPx = 40;
    private float mScale = 1f;

    private ColorFilter mPinFilter;

    private float mPinPaddingPx;

    private float mTextYPadding;

    private Rect mBounds = new Rect();

    private Resources mRes;

    private Paint mCirclePaint;

    private float mCircleRadiusPx;

    // Constructors ////////////////////////////////////////////////////////////

    public PinView(Context context) {
        super(context);
    }

    // Initialization //////////////////////////////////////////////////////////

    /**
     * The view is created empty with a default constructor. Use init to set all the initial
     * variables for the pin
     *
     * @param ctx          Context
     * @param y            The y coordinate to raw the pin (i.e. the bar location)
     * @param pinColor     the color of the pin
     * @param textColor    the color of the value text in the pin
     * @param circleRadius the radius of the selector circle
     * @param circleColor  the color of the selector circle
     */
    public void init(Context ctx, float y, int pinColor, int textColor,
                     float circleRadius, int circleColor) {
        mRes = ctx.getResources();
        mPin = ctx.getResources().getDrawable(R.drawable.rangebar_pin_rect);

        mPinPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2, mRes.getDisplayMetrics());
        mCircleRadiusPx = circleRadius;
        mTextYPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                3.5f, mRes.getDisplayMetrics());
        //Set text size in px from dp
        int textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                15, mRes.getDisplayMetrics());

        // Creates the paint and sets the Paint values
        mTextPaint = new Paint();
        mTextPaint.setColor(textColor);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(textSize);
        // Creates the paint and sets the Paint values
        mCirclePaint = new Paint();
        mCirclePaint.setColor(circleColor);
        mCirclePaint.setAntiAlias(true);

        //Color filter for the selection pin
        mPinFilter = new LightingColorFilter(pinColor, pinColor);

        // Sets the minimum touchable area, but allows it to expand based on
        // image size
        int targetRadius = (int) Math.max(MINIMUM_TARGET_RADIUS_DP, Math.min(mPinHeightPx, mPinWidthPx));

        mTargetRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                (float)targetRadius * 1.5f,
                mRes.getDisplayMetrics());
        mY = y;
    }

    public void setPinDims(int pinWidthPx, int pinHeightPx) {
        // If one of the attributes are set, but the others aren't, set the
        // attributes to default
        mPinWidthPx = pinWidthPx;
        mPinHeightPx = pinHeightPx;
    }

    public void setPinPadding(int pinPadding) {
        mPinPaddingPx = pinPadding;
    }

    /**
     * Set the x value of the pin
     *
     * @param x set x value of the pin
     */
    @Override
    public void setX(float x) {
        mX = x;
    }


    /**
     * Get the x value of the pin
     *
     * @return x float value of the pin
     */
    @Override
    public float getX() {
        return mX;
    }


    /**
     * Set the value of the pin
     *
     * @param x String value of the pin
     */
    public void setXValue(String x) {
        mValue = x;
    }

    /**
     * Determine if the pin is pressed
     *
     * @return true if is in pressed state
     * false otherwise
     */
    @Override
    public boolean isPressed() {
        return mIsPressed;
    }

    /**
     * Sets the state of the pin to pressed
     */
    public void press() {
        mIsPressed = true;
    }

    /**
     * Set size of the pin and padding for use when animating pin enlargement on press
     *
     */
    public void setScale(float scale) {
        mScale = scale;
        invalidate();
    }

    /**
     * Release the pin, sets pressed state to false
     */
    public void release() {
        mIsPressed = false;
    }

    /**
     * Determines if the input coordinate is close enough to this thumb to
     * consider it a press.
     *
     * @param x the x-coordinate of the user touch
     * @param y the y-coordinate of the user touch
     * @return true if the coordinates are within this thumb's target area;
     * false otherwise
     */
    public boolean isInTargetZone(float x, float y) {
        return (Math.abs(x - mX) <= mTargetRadiusPx
                && Math.abs(y - mY + mPinPaddingPx) <= mTargetRadiusPx);
    }

    //Draw the circle regardless of pressed state. If pin size is >0 then also draw the pin and text
    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawCircle(
                Math.min(canvas.getWidth() - mCircleRadiusPx, Math.max(mX, mCircleRadiusPx)),
                mY, mCircleRadiusPx, mCirclePaint);

        //Draw pin if pressed
        if (mScale > 0f && mPinHeightPx > 0 && mPinWidthPx > 0) {
            int wScaledPx = getScaledPinWidth();
            int hScaledPx = getScaledPinHeight();
            int paddingScaled = getScaledPinPadding();
            int pinBottomMargin = mPinHeightPx / 2;

            mBounds.set(
                    (int) mX - (wScaledPx/2),
                    (int) mY - pinBottomMargin - hScaledPx,
                    (int) mX + wScaledPx - (wScaledPx / 2),
                    (int) mY - pinBottomMargin);

            mPin.setBounds(mBounds);
            String text = mValue;
            if (mValue.length() > 8) {
                text = mValue.substring(0, 8);
            }

            int textSize = hScaledPx - paddingScaled - paddingScaled;
            mTextPaint.setTextSize(textSize);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mPin.setColorFilter(mPinFilter);
            mPin.draw(canvas);
            canvas.drawText(text,
                    mX, mY - pinBottomMargin - (textSize / 5) - paddingScaled,
                    mTextPaint);
        }
    }

    private int getScaledPinPadding() {
        return (int) (mPinPaddingPx * mScale);
    }

    private int getScaledPinHeight() {
        return (int) (((float)mPinHeightPx) * mScale);
    }

    private int getScaledPinWidth() {
        return (int) (((float)mPinWidthPx) * mScale);
    }

    // Private Methods /////////////////////////////////////////////////////////////////

    //Set text size based on available pin width.
    private static void _calibrateTextSize(Paint paint, String text, float min, float max,
                                          float boxWidth) {
        float size = Math.max(Math.min((boxWidth / paint.measureText(text)) * 10, max), min);
        paint.setTextSize(size);
    }
}
