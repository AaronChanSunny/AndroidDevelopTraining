package com.aaron.androiddeveloptraining;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by Aaron on 2016/3/16.
 */
public class BitmapUtil {
    public static Bitmap decodeBitmapFromResource(Resources res, int resId, int reqWidth, int
            reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, R.drawable.android,
                options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int
            reqHeight) {
        int sampleSize = 1;
        final int height = options.outHeight;
        final int width = options.outWidth;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while (halfHeight / sampleSize > reqHeight && halfWidth / sampleSize > reqWidth) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }
}
