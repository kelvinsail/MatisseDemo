package com.example.cropapplication

import android.app.Application

/**
 * @author yifan
 *
 * created in 2020-01-13.
 *
 * used for
 */
class CropApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {

        lateinit var instance: CropApplication

        fun instance() = instance
    }
}