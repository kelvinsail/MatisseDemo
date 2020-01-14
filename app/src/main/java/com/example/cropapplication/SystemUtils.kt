package com.example.cropapplication

import android.os.Build
import android.os.Environment
import com.blankj.utilcode.util.LogUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * @author yifan
 *
 *
 * created in 2020-01-13.
 *
 *
 * used for
 */
object SystemUtils {
    const val TAG = "SystemUtils"

    /**
     * 判断是否小米
     */
    fun isMIUI(): Boolean {
        val manufacturer = Build.MANUFACTURER
        LogUtils.dTag(TAG, "onCreated isMIUI: $manufacturer")
        //这个字符串可以自己定义,例如判断华为就填写huawei,魅族就填写meizu
        if ("xiaomi" == (manufacturer.toLowerCase(Locale.getDefault()))) {
            return true
        }
        return false
    }

}
