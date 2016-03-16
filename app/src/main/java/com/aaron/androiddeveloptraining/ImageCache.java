package com.aaron.androiddeveloptraining;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by Aaron on 2016/3/16.
 */
public class ImageCache extends LruCache<String, Bitmap> {

    public ImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount() / 1024;
    }
}
