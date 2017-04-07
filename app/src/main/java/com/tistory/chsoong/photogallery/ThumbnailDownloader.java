package com.tistory.chsoong.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by chsoong on 2017. 4. 5..
 */

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private LruCache mCache = new LruCache(100);

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if(url == null) {
            mRequestMap.remove(target);
        }
        else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public ThumbnailDownloader(String name) {
        super(name);
    }

//    public ThumbnailDownloader(String name, int priority) {
//        super(name, priority);
//    }
//
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleImageRequest(target);
                }

                super.handleMessage(msg);
            }
        };
    }

    private Bitmap checkCache(String key){
        return (Bitmap)mCache.get(key);
    }

    private void handleImageRequest(final T target) {

        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }

            Bitmap cachedBitmap = checkCache(url);
            if(cachedBitmap == null) {
                byte[] bitmapBytes = new FlickrFetch().getUrlBytes(url);
                cachedBitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mCache.put(url, cachedBitmap);

                Log.i(TAG, "Bitmap Created");
            }
            else {
                Log.i(TAG, "cache HIT !!!");
            }

            final Bitmap bitmap = cachedBitmap;
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url) {
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });

        } catch (IOException ioe){
            Log.e(TAG, "Error donwloading image", ioe);

        }
    }
}
