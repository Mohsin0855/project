package com.example.project.activity

import android.app.Application
import com.example.project.util.AppOpenAdManager
import com.google.android.gms.ads.MobileAds

class MyApp : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager(this)
    }
}