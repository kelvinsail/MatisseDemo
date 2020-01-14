package com.example.cropapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import com.zxy.tiny.Tiny;
import com.zxy.tiny.common.BitmapResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 *
 */
public class BitmapUtils {

    /**
     * 图片压缩
     *
     * return byte[]
     */
    public static byte[] compressByQuality(String filePath, long maxByteSize) {
        return compressByQuality(new File(filePath),maxByteSize);
    }


    /**
     * 图片压缩
     *
     * return byte[]
     */
    public static byte[] compressByQuality(File file, long maxByteSize) {
        Tiny.FileCompressOptions options = new Tiny.FileCompressOptions();
        options.size = maxByteSize;
        BitmapResult result = Tiny.getInstance().source(file).asBitmap().withOptions(options).compressSync();

        if (result.success && null != result.bitmap) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        }
        return null;
    }



    /**
     * 图片压缩
     *
     * return byte[]
     */
    public static byte[] compressByQuality(Bitmap bitmap, long maxByteSize) {
        Tiny.FileCompressOptions options = new Tiny.FileCompressOptions();
        options.size = maxByteSize;
        BitmapResult result = Tiny.getInstance().source(bitmap).asBitmap().withOptions(options).compressSync();

        if (result.success && null != result.bitmap) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        }
        return null;
    }

    /**
     * 图片压缩
     *
     * return byte[]
     */
    public static byte[] compressByQuality(Uri uri, long maxByteSize) {
        Tiny.FileCompressOptions options = new Tiny.FileCompressOptions();
        options.size = maxByteSize;
        BitmapResult result = Tiny.getInstance().source(uri).asBitmap().withOptions(options).compressSync();
        if (result.success && null != result.bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            //Bugtags.sendException(new Exception("API：" + Build.VERSION.SDK_INT + " ,uri取图解析失败: " + uri.getPath()));
            return baos.toByteArray();
        }
        return null;
    }




    public static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

        // Get EGL Display
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
                maximumTextureSize = textureSize[0];
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri, BitmapFactory.Options opts) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            if (null == fileDescriptor) {
                return null;
            }
            Bitmap image = null;
            if (null != opts) {
                image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, opts);
            } else {
                image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从路劲获取URI
     *
     * @param context
     * @param path
     * @return
     */
    public static Uri getImageContentUri(Context context, String path) {
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{path}, null);
        Uri uri = null;
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            uri = Uri.withAppendedPath(baseUri, "" + id);
        } else {
            // 如果图片不在手机的共享图片数据库，就先把它插入。
            if (new File(path).exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, path);
                uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }
        }
        if (null != cursor) {
            cursor.close();
        }
        return uri;
    }
}
