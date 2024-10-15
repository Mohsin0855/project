package com.example.project.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(private val myApplication: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var isAdShowing = false
    private var currentActivity: Activity? = null
    private var hasShownAd = false // Track whether the ad has been shown
    private var isAppClosed = false // Track if the app was closed

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        loadAd() // Load the ad initially
    }

    fun loadAd() {
        if (isAdLoading || isAdAvailable()) {
            return
        }
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            myApplication,
            "ca-app-pub-3940256099942544/9257395921", // Replace with your actual App Open Ad Unit ID
            adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAdLoading = false
                    Log.d("AppOpenAdManager", "Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    Log.d("AppOpenAdManager", "Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    fun showAdIfAvailable() {
        if (!isAdShowing && isAdAvailable() && !hasShownAd && isAppClosed) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isAdShowing = false
                    hasShownAd = true // Set the flag after showing the ad
                    Log.d("AppOpenAdManager", "Ad dismissed.")
                    loadAd() // Load a new ad for the next time
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isAdShowing = false
                    Log.d("AppOpenAdManager", "Ad failed to show.")
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isAdShowing = true
                    Log.d("AppOpenAdManager", "Ad showed.")
                }
            }
            currentActivity?.let {
                appOpenAd?.show(it)
                Log.d("AppOpenAdManager", "Attempting to show ad.")
            }
        } else {
            Log.d("AppOpenAdManager", "Ad not ready or already showing.")
        }
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        // Check if the app is being launched from scratch (i.e., not in the background)
        if (activity.isTaskRoot) {
            // Reset the flag for the next launch
            isAppClosed = true // Set this to true when the app is opened fresh
            showAdIfAvailable() // Show the ad if conditions are met
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            isAppClosed = false // Reset the flag when the activity stops
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
