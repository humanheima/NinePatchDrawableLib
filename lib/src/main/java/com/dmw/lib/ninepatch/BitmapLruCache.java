package com.dmw.lib.ninepatch;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Created by dmw on 2023/10/30
 * Desc: 内存缓存，存储bitmap
 * key：bitmap对应的文件路径
 */
public class BitmapLruCache {

    private static final String TAG = "BitmapLruCache";

    private LruCache<String, Bitmap> cache;

    public BitmapLruCache() {
        // Calculate the maximum memory available to the cache
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Set the cache size to be a fraction of the available memory
        int cacheSize = maxMemory / 8;

        // Initialize the LruCache with the calculated cache size
        cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // Return the size of the bitmap in kilobytes
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void putBitmap(String key, Bitmap bitmap) {
        cache.put(key, bitmap);
    }

    public Bitmap getBitmap(String key) {
        return cache.get(key);
    }

    public void clearCache() {
        cache.evictAll();
    }
}