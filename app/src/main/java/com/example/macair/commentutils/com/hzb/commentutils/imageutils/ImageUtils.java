package com.example.macair.commentutils.com.hzb.commentutils.imageutils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.util.ResourceBundle;

/**
 * Created by macair on 16/3/29.
 */
public class ImageUtils {

    /**
     * 旋转图片
     * @param bitmap
     * @param orientation
     * @return bitmap
     */
    public static Bitmap getRotatebBitmap (Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();

        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;

            default:
                break;

        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);

        return bitmap;
    }

    /**
     * 计算insamplesize的值
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return InSampleSize
     */
    private static int calculateInSampleSize (BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final  int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final  int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap createScaleBitmap (Bitmap bitmap, int dstWidth, int dstHeight) {
        Bitmap dst = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false);
        if (bitmap != dst) {
            bitmap.recycle();
        }
        return dst;
    }

    /**
     * 从resource读取并返回指定高度、图片
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    
    public static Bitmap decodeSampledBitmapFromResource (Resources res, int resId, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        //设置首次decode只读取边界信息
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeResource(res, resId, options);
        return createScaleBitmap(src, reqWidth, reqHeight);

    }

    public static Bitmap decodeSampledBitmapFromFd (String pathName, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeFile(pathName, options);
        return createScaleBitmap(src, reqWidth, reqHeight);
    }


}
