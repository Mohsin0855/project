package com.example.project.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.project.R
import com.example.project.adaptor.ViewPagerAdapter
import com.example.project.repo.UserRepository
import com.example.project.util.AppOpenAdManager
import com.example.project.util.UserViewModelFactory
import com.example.project.viewmodel.UserViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    // Expose the ViewModelFactory for Fragments
    lateinit var userViewModelFactory: UserViewModelFactory
    private lateinit var userViewModel: UserViewModel
    private lateinit var appOpenAdManager: AppOpenAdManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Mobile Ads
        MobileAds.initialize(this) {}

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize the repository
        val repository = UserRepository()

        // Create the ViewModelFactory
        userViewModelFactory = UserViewModelFactory(repository)

        // Initialize the ViewModel
        userViewModel = ViewModelProvider(this, userViewModelFactory)[UserViewModel::class.java]

        // Initialize the App Open Ad Manager
        appOpenAdManager = (application as MyApp).appOpenAdManager

        // Show the App Open Ad
        appOpenAdManager.showAdIfAvailable() // Show ad here
        // Set up the ViewPager and TabLayout
        setupViewPagerAndTabs()
        userViewModel.loadInterstitialAd(this)
    }

    private fun setupViewPagerAndTabs() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        // Set the adapter for the ViewPager
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Connect the TabLayout with the ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Home"
                1 -> tab.text = "Favourites"
                2 -> tab.text = "Archive"
            }
        }.attach()
    }
}
