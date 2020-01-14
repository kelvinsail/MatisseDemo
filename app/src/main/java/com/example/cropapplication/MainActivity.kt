package com.example.cropapplication

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.blankj.utilcode.constant.MemoryConstants
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        private const val TAKE_PHOTO = 212 //拍照返回的请求码
        private const val REQUEST_CODE_CHOOSE = 222 //选择图片的请求码
        private const val REQUEST_CODE_CROP = 0x0002
        private const val KEY_IMAGE_URI_FROM_FILE = "key_image_uri_from_file"
        private const val KEY_IMAGE_FILE_PATH = "key_image_file_path"
    }

    private var mImageUriFromFile: Uri? = null
    private var mImageFilePath: String? = null

    //备选，适配小米
    private var mCropImageUri: Uri? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //发现在小米9 android10上，调起系统裁剪功能之后，activity直接被回收了
        outState.putParcelable(MediaStore.EXTRA_OUTPUT, mCropImageUri)
        outState.putParcelable(KEY_IMAGE_URI_FROM_FILE, mImageUriFromFile)
        outState.putString(KEY_IMAGE_FILE_PATH, mImageFilePath)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //发现在小米9 android10上，调起系统裁剪功能之后，activity直接被回收了
        savedInstanceState?.let { bundle ->
            mCropImageUri = bundle.getParcelable(MediaStore.EXTRA_OUTPUT)
            mImageUriFromFile = bundle.getParcelable(KEY_IMAGE_URI_FROM_FILE)
            mImageFilePath = bundle.getString(KEY_IMAGE_FILE_PATH)
        }

        setContentView(R.layout.activity_main)
        btnTakePhoto.setOnClickListener {
            btnTakePhoto.tag = 1
            selectPhoto(it as TextView)
        }
        btnTakeCamera.setOnClickListener {
            btnTakeCamera.tag = 0
            selectPhoto(it as TextView)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LogUtils.dTag(TAG, "onActivityResult: ${data?.extras.toString()}")
        data?.extras?.let { bundle ->
            bundle.keySet().forEach {
                LogUtils.dTag(TAG, "onActivityResult: $it -> ${bundle.get(it)}")
            }
        }

        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            // 选择照片完成
            data?.apply {
                val mSelected = Matisse.obtainResult(data)

                LogUtils.d("Matisse", "mSelected: " + mSelected)
                if (mSelected != null && mSelected.size > 0) {
                    cropImage(mSelected[0])
                }
            }
        } else if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            // 拍照完成
            cropImage(mImageUriFromFile)
        } else if (requestCode == REQUEST_CODE_CROP && resultCode == RESULT_OK) {
            //为了适配小米cc9.0，裁剪完压根就不返回路径跟uri
            //所以直接在传递crop组件时，提取uri保存
            LogUtils.dTag(TAG, "onActivityResult: getUri -> ${mCropImageUri?.path}")
            //在这里获得了剪裁后的Bitmap对象，可以用于上传，作为传值之一，以bitmap优先
            var bitmap = data?.extras?.getParcelable<Bitmap>("data")

            if (SystemUtils.isMIUI() && null == bitmap && null != mCropImageUri) {
                bitmap = BitmapUtils.getBitmapFromUri(this, mCropImageUri, null)
            }
            if (null != mCropImageUri || null != bitmap) {
                upLoadFile(mCropImageUri, bitmap)
            } else {
                ToastUtils.showShort(R.string.choose_fail)
            }

        }
    }

    private fun cropImage(uri: Uri?) {
        if (uri == null) {
            ToastUtils.showShort("文件不存在")
            return
        }
        val fileName = "${System.currentTimeMillis()}.jpg"
        val intent = IntentUtils.getCropIntentUri(uri, fileName)
        mCropImageUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        startActivityForResult(intent, REQUEST_CODE_CROP)
    }

    private fun selectPhoto(textView: TextView) {
        val photo = 0
        val album = 1
        when (textView.tag.toString().toInt()) {
            photo -> {
                AndPermission.with(this).runtime().permission(
                    Permission.WRITE_EXTERNAL_STORAGE,
                    Permission.READ_EXTERNAL_STORAGE, Permission.CAMERA
                )
                    .onGranted {
                        openCamera()
                    }
                    .onDenied {
                        ToastUtils.showShort(getString(R.string.permission_no_photo))
                    }
                    .start()
            }
            album -> {
                AndPermission.with(this)
                    .runtime()
                    .permission(
                        Permission.WRITE_EXTERNAL_STORAGE,
                        Permission.READ_EXTERNAL_STORAGE,
                        Permission.CAMERA
                    )
                    .onGranted {

                        Matisse.from(this)
                            .choose(MimeType.ofImage())
                            .countable(false)
                            .maxSelectable(1)
                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            .thumbnailScale(0.85f)
                            .imageEngine(Glide4Engine())
                            .forResult(REQUEST_CODE_CHOOSE)
                    }
                    .onDenied {
                        ToastUtils.showShort(getString(R.string.permission_no_photo))
                    }.start()
            }
        }
    }

    /**
     * 调起相机拍照
     */
    private fun openCamera() {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // 判断是否有相机
        if (captureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            var photoUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 适配android 10
                photoUri = createImageUri()
            } else {
                try {
                    photoFile = createImageFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                if (photoFile != null) {
                    mImageFilePath = photoFile.absolutePath
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
                        photoUri = AndPermission.getFileUri(this, photoFile)
                    } else {
                        photoUri = Uri.fromFile(photoFile)
                    }
                }
            }

            mImageUriFromFile = photoUri
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(captureIntent, TAKE_PHOTO)
            }
        }
    }

    /**
     * 创建用来存储图片的文件，以时间来命名就不会产生命名冲突
     *
     * @return 创建的图片文件
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile: File
        imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return imageFile
    }

    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     */
    private fun createImageUri(): Uri? {
        val status: String = Environment.getExternalStorageState()
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            )
        } else {
            return contentResolver.insert(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                ContentValues()
            )
        }
    }


    private fun upLoadFile(uri: Uri?, bitmap: Bitmap?) {

        //文件大小 不能超过2M
        val maxSize = 2L * MemoryConstants.MB
        var bitmapBytes: ByteArray? = null
        if (null != bitmap) {
            bitmapBytes = BitmapUtils.compressByQuality(bitmap, maxSize)
            ivImage.setImageBitmap(bitmap)
        } else {
            bitmapBytes = BitmapUtils.compressByQuality(uri, maxSize)
            ivImage.setImageURI(uri)
        }
        if (null != bitmapBytes) {
            ToastUtils.showShort("获取成功")
        } else {
            ToastUtils.showShort("获取失败")
        }
    }
}
