/******************************************************************
 * @file   ThumbDrawable.java
 * @author Richard Luo
 * @date   2015-01-30 16:37:21
 * 
 * @brief  
 * 
 ****************************************************************** 
 */

package org.jraf.android.backport.switchwidget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class ThumbDrawable extends Drawable {
    private static final String TAG = "ThumbDrawable";

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1;
    private static final int COUNT = (WIDTH + 1) * (HEIGHT + 1);

    private final float[] mVerts = new float[COUNT * 2];
    private final float[] mOrigs = new float[COUNT * 2];

    private final RectF mRect;
    private final Paint mPaint;

    private final Drawable      mThumbDrawable;
    private final Bitmap        mThumbBitmap;

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


    private void squashMatrix() {
        int index = 0;
        final float dia = mThumbDrawable.getIntrinsicWidth();
        final float width = dia + (dia * getLevel()/ 10000);
        for (int y = 0; y <= HEIGHT; y++) {
            float fx = 0.0f;
            float fy = dia * y / HEIGHT;
            for (int x = 0; x <= WIDTH; x++) {
                fx += dia / WIDTH;
                if (x == WIDTH/2) {
                    fx += (width - dia);
                }
                setXY(mVerts, index, fx, fy);
                index += 1;
            }
        }
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

    private int mPosition = 0;

    private final int mMaxSquashRatio;

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int posi) {
        mPosition = posi;
    }

    public ThumbDrawable(Drawable drawable, int squash_ratio) {
        mRect = new RectF();
        mPaint = new Paint();
        mMaxSquashRatio = squash_ratio;
        mThumbDrawable = drawable;
        if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable bm = (BitmapDrawable) drawable;
            mThumbBitmap = bm.getBitmap();
            // mThumbBitmap = Bitmap.createScaledBitmap(bm.getBitmap(), dia, dia, false);
        } else {
            throw new IllegalArgumentException("only support BitmapDrawable now!");
        }
    }

    // public void setBounds(int left, int top, int right, int bottom) {
//         super.setBounds(left, top, right, bottom);
//     }

    @Override
    protected boolean onLevelChange(int level) {
        return false;
    }

    @Override
    public int getIntrinsicWidth() {
        final int w = mThumbDrawable.getIntrinsicWidth();
        return w + (w * mMaxSquashRatio / 10000);
    }

    @Override
    public int getIntrinsicHeight() {
        return mThumbDrawable.getIntrinsicHeight();
    }

    private int getActualWidth() {
        final int w = mThumbDrawable.getIntrinsicWidth();
        return w + (w * getLevel() / 10000);
    }

    @Override
    public void draw(Canvas canvas) {
        final int distance = getIntrinsicWidth() - getActualWidth();
        final int x = distance * mPosition / 10000;
        canvas.save();
        canvas.translate(x, 0);
        squashMatrix();
        canvas.drawBitmapMesh(mThumbBitmap, WIDTH, HEIGHT, mVerts, 0, null, 0, null);
        canvas.restore();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int arg0) {
    }

    @Override
    public void setColorFilter(ColorFilter arg0) {
    }
    
}


