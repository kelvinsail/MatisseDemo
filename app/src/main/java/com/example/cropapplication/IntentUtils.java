package com.example.cropapplication;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.blankj.utilcode.util.ToastUtils;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.io.File;
import java.util.List;

public class IntentUtils {

    private static final String TAG = "IntentUtils";

    /**
     * 裁剪图片
     *
     * @param outputImage
     * @param formUri
     * @return
     */
    public static Intent getCropIntent(File outputImage, Uri formUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(formUri, "image/*");
        // 授权应用读取 Uri，这一步要有，不然裁剪程序会崩溃
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputImage));
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        //前置摄像头
        intent.putExtra("noFaceDetection", true);
        intent = Intent.createChooser(intent, "裁剪图片");
        return intent;
    }

    /**
     * 裁剪图片
     *
     * @param formUri
     * @return
     */
    public static Intent getCropIntentUri(Uri formUri, String fileName) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(formUri, "image/*");
        // 授权应用读取 Uri，这一步要有，不然裁剪程序会崩溃
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("scale", true);

        Uri uri = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || SystemUtils.INSTANCE.isMIUI()) {
            intent.putExtra("return-data", false);

            if (TextUtils.isEmpty(fileName)) {
                fileName = System.currentTimeMillis() + ".jpg";
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Images.Media.DESCRIPTION, fileName);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            uri = CropApplication.Companion.getInstance().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        } else {
            //android10通过onActivityResult返回的bitmap处理
            intent.putExtra("return-data", true);
        }

        int count = CropApplication.Companion.getInstance().getPackageManager()
                .queryIntentActivities(intent, PackageManager.GET_ACTIVITIES).size();
        if (count > 0) {
            intent = Intent.createChooser(intent, "裁剪图片");
            //再放一次
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        return intent;
    }

    /**
     * 选择图片，并进行裁切注意 onactivityresult 获取获取图片路径
     * 裁切之后无uri
     */
    public static void selectPhotoCrop(final Activity activity, final int requestCode) {
        AndPermission.with(activity)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        Matisse.from(activity)
                                .choose(MimeType.ofImage())
                                .showSingleMediaType(true)
                                .theme(R.style.Matisse_Dracula)
                                .thumbnailScale(0.7f)
                                .imageEngine(new Glide4Engine())
                                .forResult(requestCode);
                    }
                })
                .onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        ToastUtils.showShort(activity.getString(R.string.permission_no_photo));
                    }
                }).start();
    }

}
