package com.example.macair.commentutils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by huangzhebin on 16/4/1.
 */
public class Utils {

    public static String getProcessName(Context context, int pid) {
        //先获得activitymanager
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcessInfos = am.getRunningAppProcesses();
        if (runningAppProcessInfos == null) {
            return null;
        } else {
            for (RunningAppProcessInfo info : runningAppProcessInfos) {
                if (info.pid == pid)
                    return info.processName;
            }
        }
        return null;

    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
            ;
        }
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    public static float dp2px(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getDisplayMetrics(context));
    }

    public static boolean isWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;

    }

    public static void print(byte[] byts) {
        // #ifdef DEBUG
        if (byts == null) {
            return;
        } else {
            for (int i = 0; i < byts.length; i++) {
                System.out.println("[" + i + "]" + ":\t");
                System.out.println(byts[i]);
            }
        }
        // #ifdef DEBUG
    }

    public static void print(String s) {
        // #ifdef DEBUG
        if (!TextUtils.isEmpty(s)) {
            int length = s.length();
            int offset = 3000;
            if (length > offset) {
                int n = 0;
                for (int i = 0; i < length; i += offset) {
                    n += offset;
                    if (n > length)
                        n = length;
                    System.err.println("Debug = " + s.substring(i, n));
                }
            } else {
                System.out.println(s);
            }
        }
        // #ifdef DEBUG
    }

    public static void print(Object obj) {
        if (obj != null) {
            if (obj instanceof byte[]) {
                print(obj);
            } else if (obj instanceof String) {
                print(obj);
            } else {
                System.out.println(obj);
            }
        }
    }

    public static File getDiskCecheDir (Context context, String uniqueueName) {
        boolean externalStorageAvailable = Environment.getExternalStorageState().endsWith(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueueName);
    }
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static long getUsableSpace (File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return file.getUsableSpace();
        }
        final StatFs statFs = new StatFs(file.getPath());
        return (long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize();
    }


}
