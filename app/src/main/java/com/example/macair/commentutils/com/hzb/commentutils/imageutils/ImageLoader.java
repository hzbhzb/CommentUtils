package com.example.macair.commentutils.com.hzb.commentutils.imageutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.example.macair.commentutils.R;
import com.example.macair.commentutils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * Created by macair on 16/4/1.
 */
public class ImageLoader {

    public static final int MESSAGE_POST_RESULT = 1;
    //线程池参数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_LIVE = 10L;

    private static final int KEY_TAG_URI = R.id.image_uri;

    //磁盘缓存参数
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    private  static final int BUFFER_SIZE = 8 * 1024;
    private  static final int CACHE_INDEX = 0;
    private boolean isDiskCacheCreated = false;

    private static final ThreadFactory threadfactory = new ThreadFactory() {
        private final AtomicInteger atomicInteger = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "imageloader#" + atomicInteger.getAndIncrement());
        }
    };

    public static Executor THREAD_POOL_EXCUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_LIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadfactory);

    private  Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult)msg.obj;
            ImageView iv = result.imageView;
            String tag = (String)iv.getTag(KEY_TAG_URI);
            if (tag.equals(result.tag)) {
                iv.setImageBitmap(result.bitmap);
            } else {
                Utils.print("bitmap is loaded, but tag has changed");
            }
        }
    };

    private Context context;
    private DiskLruCache diskLruCache;
    private LruCache<String, Bitmap> lruCache;


    public ImageLoader (Context context) {
        this.context = context.getApplicationContext();
        int maxMemory = (int)Runtime.getRuntime().maxMemory() / 1024;
        int cacheSize = maxMemory / 8;
        lruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        File discacheDir = Utils.getDiskCecheDir(context, "bitmap");
        if (!discacheDir.exists()) {
            discacheDir.mkdirs();
        }
        if (Utils.getUsableSpace(discacheDir) > DISK_CACHE_SIZE) {
            try {
                diskLruCache = DiskLruCache.open(discacheDir, 1, 1, DISK_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private Bitmap getBitmapFromLruCache (String key) {
        return lruCache.get(key);
    }

    private void addBitmap2LruCache (Bitmap bitmap, String key) {

        if (getBitmapFromLruCache(key) == null) {
            lruCache.put(key, bitmap);
        }
    }

    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight) {
        imageView.setTag(KEY_TAG_URI, uri);
        Bitmap bitmap = getBitmapFromLruCache(uri);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }

        Runnable task = new Runnable() {
            @Override
            public void run() {
                    Bitmap resultBitmap = loadBitmap(uri, reqWidth, reqHeight);
                    if (resultBitmap != null) {
                        LoaderResult result = new LoaderResult(imageView, uri, resultBitmap);
                        handler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();
                    }
            }


        };
        THREAD_POOL_EXCUTOR.execute(task);
    }

    public void bindBitmap(String uri, ImageView imageView) {
        bindBitmap(uri, imageView, 0, 0);
    }

    private Bitmap loadBitmap(String uri, int reqWidth, int reqHeight)  {

        Bitmap bitmap = null;
        try {
            bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
            if (bitmap != null) {
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (bitmap == null && !isDiskCacheCreated) {
            bitmap = loadBitmapFromUrl(uri, reqWidth, reqHeight);
        }
        return null;
    }

    private Bitmap loadBitmapFromUrl(String uri, int reqWidth, int reqHeight) {
        Bitmap bitmap = null;
        BufferedInputStream bufferedInputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(uri);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(bufferedInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();
            Utils.close(bufferedInputStream);
        }
        return bitmap;
    }

    private Bitmap loadBitmapFromHttp(String uri, int reqWidth, int reqHeight) throws IOException{

        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("visit network in ui thread available");
        }
        if (diskLruCache == null) {
            return null;
        }
        String key = Utils.hexKeyFromUrl(uri);
        DiskLruCache.Editor editor = diskLruCache.edit(key);
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(CACHE_INDEX);
            if (downloadUrl2Stream(uri, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            diskLruCache.flush();
        }
        return loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
    }

    private boolean downloadUrl2Stream(String uri, OutputStream outputStream) {
        HttpURLConnection httpURLConnection = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            URL url = new URL(uri);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), BUFFER_SIZE);
            bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER_SIZE);
            int k;
            while((k = bufferedInputStream.read()) != -1)
                bufferedOutputStream.write(k);
            return true;
        } catch (Exception e) {
            e.printStackTrace();;
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            Utils.close(bufferedInputStream);
            Utils.close(bufferedOutputStream);
        }
        return false;
    }

    private Bitmap loadBitmapFromDiskCache(String uri, int reqWidth, int reqHeight) throws IOException{

        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("visit network in ui thread available");
        }

        Bitmap bitmap = null;
        String key = Utils.hexKeyFromUrl(uri);
        DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream fileInputStream = (FileInputStream)snapshot.getInputStream(CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = ImageUtils.decodeSampledBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            if (bitmap != null)
                addBitmap2LruCache(bitmap, uri);
        }
        return bitmap;
    }

    private  class LoaderResult {
        public ImageView imageView;
        public String tag;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageview, String tag, Bitmap bitmap) {
            this.bitmap = bitmap;
            this.tag = tag;
            this.imageView = imageview;
        }

    }



}
