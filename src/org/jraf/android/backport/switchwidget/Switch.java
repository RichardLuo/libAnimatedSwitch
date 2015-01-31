/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2012 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jraf.android.backport.switchwidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.CompoundButton;

import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;

/**
 * A Switch is a two-state toggle switch widget that can select between two
 * options. The user may drag the "thumb" back and forth to choose the selected option,
 * or simply tap to toggle as if it were a checkbox. The {@link #setText(CharSequence) text} property controls the text displayed in the label for the switch,
 * whereas the {@link #setTextOff(CharSequence) off} and {@link #setTextOn(CharSequence) on} text
 * controls the text on the thumb. Similarly, the {@link #setTextAppearance(android.content.Context, int) textAppearance} and the related
 * setTypeface() methods control the typeface and style of label text, whereas the {@link #setSwitchTextAppearance(android.content.Context, int)
 * asb_switchTextAppearance} and
 * the related seSwitchTypeface() methods control that of the thumb.
 * 
 */
public class Switch extends CompoundButton
        implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    
    private static final String TAG = "Switch";
    private boolean mCheckedAnim = true;
    private int mThumbDistance = 0;

    enum ThumbState {
        TS_STOPPED,
                TS_SQUASHING,
                TS_SLIDING,
                }

    private ThumbState mThumbState;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    // Enum for the "typeface" XML parameter.
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    private final ThumbDrawable mThumbDrawable;
    private float mThumbSquashRatio = 0.3f;
    private Bitmap mThumbBitmap;
    private final int mThumbDrawableShadowOffset;
    private final Drawable mOnTrackDrawable;
    private final Drawable mOffTrackDrawable;
    private final int mThumbTextPadding;
    private final int mSwitchMinWidth;
    private final int mSwitchPadding;
    private CharSequence mTextOn;
    private CharSequence mTextOff;

    private ShapeDrawable mBottomLayer;
    private final int mBottomLayerColor;

    private int mTouchMode;
    private final int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private final int mMinFlingVelocity;

    private float mThumbPosition;
    private int mSwitchWidth;
    private int mSwitchHeight;
    private int mSwitchHeightWithShadow;
    private int mThumbWidth; // Does not include padding
    private int mThumbHeight; // Does not include padding

    private int mSwitchLeft;
    private int mSwitchTop;
    private int mSwitchRight;
    private int mSwitchBottom;

    private final TextPaint mTextPaint;
    private ColorStateList mTextColors;
    private Layout mOnLayout;
    private Layout mOffLayout;

    private final Rect mTempRect = new Rect();

    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
    private float mRoundRadius;
    private boolean mChecked = false;
    private boolean mInvalidate = false;
    
    private void init() {
        mBottomLayer = new ShapeDrawable();
        mBottomLayer.getPaint().setAntiAlias(true);
        mBottomLayer.getPaint().setStyle(Paint.Style.FILL);
        mBottomLayer.getPaint().setColor(mBottomLayerColor);
        mThumbState = ThumbState.TS_STOPPED;
        mInvalidate = true;
        // setBackground(mBottomLayer);
    }

    @Override
    public void postInvalidate() {
        if (mInvalidate) {
            super.postInvalidate();
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        // super.setBackgroundColor(color);
        Log.w(TAG, "-->setBackColor: " + color);
        mBottomLayer.getPaint().setColor(color);
    }

    /**
     * Construct a new Switch with default styling.
     * 
     * @param context The Context that will determine this widget's theming.
     */
    public Switch(Context context) {
        this(context, null);
        init();
    }

    /**
     * Construct a new Switch with default styling, overriding specific style
     * attributes as requested.
     * 
     * @param context The Context that will determine this widget's theming.
     * @param attrs Specification of attributes that should deviate from default styling.
     */
    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.asb_switchStyle);
        init();
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     * 
     * @param context The Context that will determine this widget's theming.
     * @param attrs Specification of attributes that should deviate from the default styling.
     * @param defStyle An attribute ID within the active theme containing a reference to the
     *            default style for this widget. e.g. android.R.attr.switchStyle.
     */
    public Switch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final Resources res = getResources();
        mTextPaint.density = res.getDisplayMetrics().density;
        // XXX Was on the Android source, but had to comment it out (doesn't exist in 2.1). -- BoD
        // mTextPaint.setCompatibilityScaling(res.getCompatibilityInfo().applicationScale);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Switch, defStyle, 0);

        mThumbDrawable = new ThumbDrawable(a.getDrawable(R.styleable.Switch_asb_thumb), 2000);
        mThumbDrawableShadowOffset = a.getDimensionPixelSize(R.styleable.Switch_asb_thumbShadowOffset, 0);
        mOnTrackDrawable = a.getDrawable(R.styleable.Switch_asb_onTrack);
        mOffTrackDrawable = a.getDrawable(R.styleable.Switch_asb_offTrack);
        mTextOn = a.getText(R.styleable.Switch_asb_textOn);
        mTextOff = a.getText(R.styleable.Switch_asb_textOff);
        mThumbTextPadding = a.getDimensionPixelSize(R.styleable.Switch_asb_thumbTextPadding, 0);
        mSwitchMinWidth = a.getDimensionPixelSize(R.styleable.Switch_asb_switchMinWidth, 0);
        mSwitchPadding = a.getDimensionPixelSize(R.styleable.Switch_asb_switchPadding, 0);
        mBottomLayerColor = a.getColor(R.styleable.Switch_asb_trackColor, 0);

        final int appearance = a.getResourceId(R.styleable.Switch_asb_switchTextAppearance, 0);
        if (appearance != 0) {
            setSwitchTextAppearance(context, appearance);
        }
        a.recycle();

        final ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // Refresh display with current params
        refreshDrawableState();
        init();
    }

    /**
     * Sets the switch text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     */
    public void setSwitchTextAppearance(Context context, int resid) {
        final TypedArray appearance = context.obtainStyledAttributes(resid, R.styleable.Android);

        ColorStateList colors;
        int ts;

        colors = appearance.getColorStateList(R.styleable.Android_android_textColor);
        if (colors != null) {
            mTextColors = colors;
        } else {
            // If no color set in TextAppearance, default to the view's textColor
            mTextColors = getTextColors();
        }

        ts = appearance.getDimensionPixelSize(R.styleable.Android_android_textSize, 0);
        if (ts != 0) {
            if (ts != mTextPaint.getTextSize()) {
                mTextPaint.setTextSize(ts);
                requestLayout();
            }
        }

        int typefaceIndex, styleIndex;

        typefaceIndex = appearance.getInt(R.styleable.Android_android_typeface, -1);
        styleIndex = appearance.getInt(R.styleable.Android_android_textStyle, -1);

        setSwitchTypefaceByIndex(typefaceIndex, styleIndex);

        appearance.recycle();
    }

    private void setSwitchTypefaceByIndex(int typefaceIndex, int styleIndex) {
        Typeface tf = null;
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;

            case SERIF:
                tf = Typeface.SERIF;
                break;

            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
        }

        setSwitchTypeface(tf, styleIndex);
    }

    /**
     * Sets the typeface and style in which the text should be displayed on the
     * switch, and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     */
    public void setSwitchTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setSwitchTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            final int typefaceStyle = tf != null ? tf.getStyle() : 0;
            final int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setSwitchTypeface(tf);
        }
    }

    /**
     * Sets the typeface in which the text should be displayed on the switch.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use {@link #setSwitchTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     * 
     * @attr ref android.R.styleable#TextView_typeface
     * @attr ref android.R.styleable#TextView_textStyle
     */
    public void setSwitchTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            requestLayout();
            invalidate();
        }
    }

    /**
     * Returns the text displayed when the button is in the checked state.
     */
    public CharSequence getTextOn() {
        return mTextOn;
    }

    /**
     * Sets the text displayed when the button is in the checked state.
     */
    public void setTextOn(CharSequence textOn) {
        mTextOn = textOn;
        requestLayout();
    }

    /**
     * Returns the text displayed when the button is not in the checked state.
     */
    public CharSequence getTextOff() {
        return mTextOff;
    }

    /**
     * Sets the text displayed when the button is not in the checked state.
     */
    public void setTextOff(CharSequence textOff) {
        mTextOff = textOff;
        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);
        float r = h/2.0f;
        if (mRoundRadius != r) {
            mBottomLayer.setShape(new RoundRectShape(new float[]{r, r, r, r, r, r, r, r},
                                                     null, null));
        }
    }

    @Override
    @SuppressLint("NewApi")
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (mOnLayout == null) {
            mOnLayout = makeLayout(mTextOn);
        }
        if (mOffLayout == null) {
            mOffLayout = makeLayout(mTextOff);
        }

        mOnTrackDrawable.getPadding(mTempRect);
        final int maxTextWidth = Math.max(mOnLayout.getWidth(), mOffLayout.getWidth());
        // final int switchWidth = Math.max(Math.max(mSwitchMinWidth,
        //                                           maxTextWidth * 2 + mThumbTextPadding * 4 + mTempRect.left + mTempRect.right),
        //                                  mOnTrackDrawable.getIntrinsicWidth());

        final int switchWidth = mOnTrackDrawable.getIntrinsicWidth();
        final int switchHeight = mOnTrackDrawable.getIntrinsicHeight();

        // mThumbWidth = maxTextWidth + mThumbTextPadding * 2;
        mThumbWidth = mThumbDrawable.getIntrinsicWidth();
        mThumbHeight = mThumbDrawable.getIntrinsicHeight();

        mThumbDrawable.getPadding(mTempRect);
        // Log.d(TAG, "W: " + parentWidth + " H:" + parentHeight + " thumb-w:" + mThumbWidth);
        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;
        mSwitchHeightWithShadow = Math.max(mSwitchHeight, mThumbHeight+3*mThumbDrawableShadowOffset);

        // Log.d(TAG, "switch-w: " + mSwitchWidth + " switch-h: " + mSwitchHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Log.d(TAG, " measured-w: " + getMeasuredWidth()   +
        //            " measured-h: " + getMeasuredHeight() +
        //            " actual-h: " + mSwitchHeightWithShadow);

        final int measuredHeight = getMeasuredHeight();
        if (measuredHeight < mSwitchHeightWithShadow) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                setMeasuredDimension(getMeasuredWidth(), mSwitchHeightWithShadow);
            }
            else {
                setMeasuredDimension(getMeasuredWidthAndState(), mSwitchHeightWithShadow);
            }
        }
    }

    // XXX Was on the Android source, but had to comment it out (doesn't exist in 2.1). -- BoD
    // @Override
    // public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
    //     super.onPopulateAccessibilityEvent(event);
    //     if (isChecked()) {
    //         CharSequence text = mOnLayout.getText();
    //         if (TextUtils.isEmpty(text)) {
    //             text = getContext().getString(R.string.switch_on);
    //         }
    //         event.getText().add(text);
    //     } else {
    //         CharSequence text = mOffLayout.getText();
    //         if (TextUtils.isEmpty(text)) {
    //             text = getContext().getString(R.string.switch_off);
    //         }
    //         event.getText().add(text);
    //     }
    // }

    private Layout makeLayout(CharSequence text) {
        return new StaticLayout(text, mTextPaint,
                                (int) Math.ceil(Layout.getDesiredWidth(text, mTextPaint)), Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        mThumbDrawable.getPadding(mTempRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        final int thumbLeft = mSwitchLeft + (int) (mThumbPosition + 0.5f) - mTouchSlop;
        final int thumbRight = thumbLeft + mThumbWidth + mTempRect.left + mTempRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);

        final int action;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            action = ev.getAction();
        else
            action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = x;
                    mTouchY = y;
                    if (mThumbState == ThumbState.TS_STOPPED) {
                        mThumbState = ThumbState.TS_SQUASHING;
                        mAnimator1 =
                                ObjectAnimator.ofInt(mThumbDrawable, "level", 2000);
                        mAnimator1.setDuration(200);
                        mAnimator1.addListener(this);
                        mAnimator1.addUpdateListener(this);
                        mAnimator1.start();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (mTouchMode) {
                    case TOUCH_MODE_IDLE:
                        // Didn't target the thumb, treat normally.
                        break;

                    case TOUCH_MODE_DOWN: {
                        final float x = ev.getX();
                        final float y = ev.getY();
                        if (Math.abs(x - mTouchX) > mTouchSlop || Math.abs(y - mTouchY) > mTouchSlop) {
                            mTouchMode = TOUCH_MODE_DRAGGING;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            mTouchX = x;
                            mTouchY = y;
                            return true;
                        }
                        break;
                    }

                    case TOUCH_MODE_DRAGGING: {
                        final float x = ev.getX();
                        final float dx = x - mTouchX;
                        final float newPos = Math.max(0, Math.min(mThumbPosition + dx, getThumbScrollRange()));
                        if (newPos != mThumbPosition) {
                            mThumbPosition = newPos;
                            mTouchX = x;
                            invalidate();
                        }
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "-->ACTION_UP");
            case MotionEvent.ACTION_CANCEL: {
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    return true;
                }
                mTouchMode = TOUCH_MODE_IDLE;
                mVelocityTracker.clear();
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        Log.d(TAG, "--> cancelSuperTouch");
        final MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    /**
     * Called from onTouchEvent to end a drag operation.
     * 
     * @param ev Event that triggered the end of drag mode - ACTION_UP or ACTION_CANCEL
     */
    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;
        // Up and not canceled, also checks the switch has not been disabled during the drag
        final boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();

        cancelSuperTouch(ev);

        if (commitChange) {
            boolean newState;
            mVelocityTracker.computeCurrentVelocity(1000);
            final float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity) {
                newState = xvel > 0;
            } else {
                newState = getTargetCheckedState();
            }
            animateThumbToCheckedState(newState);
        } else {
            animateThumbToCheckedState(isChecked());
        }
    }

    @Override
    public void onAnimationStart(Animator animator) {
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (animator == mAnimator1) {
            Log.d(TAG, "finished anima1, mCheckedAnim" + mCheckedAnim);
            if (!mCheckedAnim) {
                startAnimation2();
            }
        } else if (animator == mAnimator2) {
        } else if (animator == mAnimator3) {
            mThumbState = ThumbState.TS_STOPPED;
            mCheckedAnim = true;
        }
    }

    public float getThumbPosition() {
        return mThumbPosition;
    }

    public void setThumbPosition(float position) {
        mThumbPosition = position;
    }

    @Override
    public void onAnimationCancel(Animator animator) {
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
    }

    private ValueAnimator mAnimator1;
    private ValueAnimator mAnimator2;
    private ValueAnimator mAnimator3;

    private void startAnimation2() {
        mAnimator2 = ObjectAnimator.ofInt(mThumbDrawable, "level", 0);
        mAnimator2.setDuration(200);
        mAnimator2.addListener(this);
        mAnimator2.addUpdateListener(this);
        final float dstPosi = !mChecked ? 0f : getThumbScrollRange();
        Log.d(TAG, "dstPosi:" + dstPosi);
        mAnimator3 = ObjectAnimator.ofFloat(this, "thumbPosition", dstPosi);
        mAnimator3.setDuration(200);
        mAnimator3.addListener(this);
        mAnimator3.addUpdateListener(this);
        mAnimator2.start();
        mAnimator3.start();
    }

    private void animateThumbToCheckedState(boolean newCheckedState) {
        // TODO animate!
        //float targetPos = newCheckedState ? 0 : getThumbScrollRange();
        //mThumbPosition = targetPos;
        Log.d(TAG, "--> animateThumbToCheckedState, newCheck:" + newCheckedState);
        if (mAnimator1 != null && mAnimator1.isRunning()) {
            Log.d(TAG, "--> wait anim1");
            mCheckedAnim = false;
            mChecked = newCheckedState;
        } else {
            Log.d(TAG, "--> startAnimation2()");
            startAnimation2();
        }
    }

    private boolean getTargetCheckedState() {
        return mThumbPosition >= getThumbScrollRange() / 2;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }
    
    @Override
    public void toggle() {
        Log.d(TAG, "--> toggle() checked: " + mChecked);
        setChecked(!mChecked);
    }

    @Override
    public void setChecked(boolean checked) {
        Log.d(TAG, "--> setChecked: " + checked);
        if (mInvalidate) {
            animateThumbToCheckedState(checked);
        }
        mChecked = checked;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Log.d(TAG, "left: " + left + " top: " + top + " right: " + right + " bottom: " + bottom);

        mThumbPosition = isChecked() ? getThumbScrollRange() : 0;

        int switchRight = getWidth() - getPaddingRight();
        int switchLeft = switchRight - mSwitchWidth;
        int switchTop = 0;
        int switchBottom = 0;

        if (getGravity() == Gravity.CENTER) {
            // Log.d(TAG, "CENTER");
            switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 - mSwitchHeight / 2;
            switchBottom = switchTop + mSwitchHeight;
            switchLeft = (getPaddingLeft() + getWidth() - getPaddingRight()) / 2 - mSwitchWidth/ 2;
            switchRight = switchLeft + mSwitchWidth;
            Log.d(TAG, "swLeft: " + switchLeft + " swRight: " + switchRight + " mSwitchWidth:" + mSwitchWidth);
        } else {
            switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK) {
                default:
                case Gravity.TOP:
                    // Log.d(TAG, "TOP");
                    switchTop = getPaddingTop();
                    switchBottom = switchTop + mSwitchHeight;
                    break;
                case Gravity.CENTER_VERTICAL:
                    // Log.d(TAG, "CENTER_VERTICAL");
                    switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 - mSwitchHeight / 2;
                    switchBottom = switchTop + mSwitchHeight;
                    break;
                case Gravity.BOTTOM:
                    // Log.d(TAG, "BOTTOM");
                    switchBottom = getHeight() - getPaddingBottom();
                    switchTop = switchBottom - mSwitchHeight;
                    break;
            }
        }

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;

        initThumbBitmap();
    }

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1;
    private static final int COUNT = (WIDTH + 1) * (HEIGHT + 1);

    private final float[] mVerts = new float[COUNT * 2];
    private final float[] mOrigs = new float[COUNT * 2];
    
    static void setXY(float[] array, int index, float x, float y) {
        array[index * 2 + 0] = x;
        array[index * 2 + 1] = y;
    }

    static float getX(float[] array, int index) {
        return array[index * 2 + 0];
    }

    static float getY(float[] array, int index) {
        return array[index * 2 + 1];
    }

    void initMatrix(float bitmap_w, float bitmap_h) {
        int index = 0;
        for (int y = 0; y <= HEIGHT; y++) {
            float fy = bitmap_h * y / HEIGHT;
            float fx = 0.0f;
            for (int x = 0; x <= WIDTH; x++) {
                fx += bitmap_w / WIDTH;
                setXY(mOrigs, index, fx, fy);
                setXY(mVerts, index, fx, fy);
                index += 1;
            }
        }
    }

    private void squashMatrix(float dia) {
        int index = 0;
        float w = dia + dia * mThumbSquashRatio;
        for (int y = 0; y <= HEIGHT; y++) {
            float fx = 0.0f;
            float fy = dia * y / HEIGHT;
            for (int x = 0; x <= WIDTH; x++) {
                fx += dia / WIDTH;
                if (x == WIDTH/2) {
                    fx += (w - dia);
                }
                setXY(mVerts, index, fx, fy);
                index += 1;
            }
        }
    }

    private void initThumbBitmap() {
        final int dia = mThumbWidth;
        // mThumbBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) mThumbDrawable).getBitmap(), dia, dia, false);
        // initMatrix(mThumbBitmap.getWidth(), mThumbBitmap.getHeight());
        // initMatrix(mThumbWidth, mThumbDrawable.getIntrinsicHeight());
    }

    private void drawTracks(int alpha, Canvas canvas) {
        final int switchLeft = mSwitchLeft;
        final int switchTop = mSwitchTop;
        final int switchRight = mSwitchRight;
        final int switchBottom = mSwitchBottom;

        mOnTrackDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        mOffTrackDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        mOnTrackDrawable.setAlpha(alpha);
        mOffTrackDrawable.setAlpha(255-alpha);

        if (alpha > 127) {
            mOffTrackDrawable.draw(canvas);
            mOnTrackDrawable.draw(canvas);
        } else {
            mOnTrackDrawable.draw(canvas);
            mOffTrackDrawable.draw(canvas);
        }
    }

    private void countThumbPosiDistance() {
        // Draw the switch
        final int switchLeft = mSwitchLeft;
        final int switchRight = mSwitchRight;

        mOnTrackDrawable.getPadding(mTempRect);
        final int switchInnerLeft = switchLeft + mTempRect.left;
        final int switchInnerRight = switchRight - mTempRect.right;

        mThumbDrawable.getPadding(mTempRect);
        final int thumbPos = (int) (mThumbPosition + 0.5f);
        final int thumbLeft = switchInnerLeft - mTempRect.left + thumbPos;
        final int thumbRight = switchInnerLeft + thumbPos + mThumbWidth + mTempRect.right;
        final int thumbWidth = (thumbRight - thumbLeft);

        mThumbDistance = (switchInnerRight - switchInnerLeft - thumbWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the switch
        final int switchLeft = mSwitchLeft;
        final int switchTop = mSwitchTop;
        final int switchRight = mSwitchRight;
        final int switchBottom = mSwitchBottom;

        // mBottomLayer.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        // mBottomLayer.draw(canvas);

        // mBottomLayer.setBounds(0, 0, getWidth(), getHeight());
        // mBottomLayer.draw(canvas);

        // mBottomLayer.setBounds(switchLeft, switchTop, switchRight, switchBottom);

        canvas.save();

        mOnTrackDrawable.getPadding(mTempRect);
        final int switchInnerLeft = switchLeft + mTempRect.left;
        final int switchInnerTop = switchTop + mTempRect.top;
        final int switchInnerRight = switchRight - mTempRect.right;
        final int switchInnerBottom = switchBottom - mTempRect.bottom;
        // canvas.clipRect(switchInnerLeft, switchTop, switchInnerRight, switchBottom + mThumbDrawableShadowOffset);
        canvas.clipRect(switchInnerLeft, switchTop, switchInnerRight, switchTop+mSwitchHeightWithShadow);

        mThumbDrawable.getPadding(mTempRect);
        // Log.d(TAG, "mThumbDrawable.getPadding: " + mTempRect);
        final int thumbPos = (int) (mThumbPosition + 0.5f);
        final int thumbLeft = switchInnerLeft - mTempRect.left + thumbPos;
        final int thumbTop = switchTop + mThumbDrawableShadowOffset;
        final int thumbRight = switchInnerLeft + thumbPos + mThumbWidth + mTempRect.right;
        final int thumbBottom = switchBottom + mThumbDrawableShadowOffset;

        final int thumbWidth = (thumbRight - thumbLeft);
        final int thumbDistance = (switchInnerRight - switchInnerLeft - thumbWidth);
        final int alpha = 255 * thumbPos / thumbDistance;

        // Log.d(TAG, "thumbPos: " + thumbPos + " switchInnerLeft: " +
        //      switchInnerLeft + " switchInnerRight: " + switchInnerRight);
        // Log.d(TAG, "thumbDistance: " + thumbDistance + " thumbWidth: " + thumbWidth);

        drawTracks(alpha, canvas);
        // mThumbDrawable.setBounds(thumbLeft, thumbTop, thumbRight, thumbBottom);
        // mThumbDrawable.draw(canvas);

        // // squashMatrix(mThumbWidth);
        // Paint paint = new Paint();
        // paint.setAntiAlias(true);
        // paint.setFilterBitmap(true);
        // paint.setDither(true);

        canvas.translate(thumbLeft, thumbTop);
        // mThumbDrawable.setLevel(alpha * 5000/255);
        mThumbDrawable.setPosition(10000 * thumbPos / thumbDistance);
        mThumbDrawable.draw(canvas);

        // canvas.drawBitmapMesh(mThumbBitmap, WIDTH, HEIGHT, mVerts, 0, null, 0, paint);

        // Paint paint = new Paint();
        // paint.setStyle(Paint.Style.FILL);
        // paint.setColor(R.color.dim_foreground_holo_dark);
        // canvas.drawRect(thumbLeft, switchTop, thumbRight, switchBottom, paint);

        // mTextColors should not be null, but just in case
        if (mTextColors != null) {
            mTextPaint.setColor(mTextColors.getColorForState(getDrawableState(), mTextColors.getDefaultColor()));
        }
        mTextPaint.drawableState = getDrawableState();

        final Layout switchText = getTargetCheckedState() ? mOnLayout : mOffLayout;

        canvas.translate((thumbLeft + thumbRight) / 2 - switchText.getWidth() / 2,
                         (switchInnerTop + switchInnerBottom) / 2 - switchText.getHeight() / 2);
        switchText.draw(canvas);

        canvas.restore();
    }

    @Override
    public int getCompoundPaddingRight() {
        int padding = super.getCompoundPaddingRight() + mSwitchWidth;
        if (!TextUtils.isEmpty(getText())) {
            padding += mSwitchPadding;
        }
        return padding;
    }

    private int getThumbScrollRange() {
        if (mOnTrackDrawable == null) {
            return 0;
        }
        mOnTrackDrawable.getPadding(mTempRect);
        return mSwitchWidth - mThumbWidth - mTempRect.left - mTempRect.right;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final int[] myDrawableState = getDrawableState();

        // Set the state of the Drawable
        // Drawable may be null when checked state is set from XML, from super constructor
        if (mThumbDrawable != null) mThumbDrawable.setState(myDrawableState);
        if (mOnTrackDrawable != null) mOnTrackDrawable.setState(myDrawableState);

        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mOnTrackDrawable;
    }

    // XXX Was on the Android source, but had to comment it out (doesn't exist in 2.1). -- BoD
    // @Override
    // public void jumpDrawablesToCurrentState() {
    //     super.jumpDrawablesToCurrentState();
    //     mThumbDrawable.jumpToCurrentState();
    //     mOnTrackDrawable.jumpToCurrentState();
    // }
}
