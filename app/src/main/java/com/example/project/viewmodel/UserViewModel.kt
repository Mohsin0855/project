package com.example.project.viewmodel

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.api.ApiService
import com.example.project.db.UserEntity
import com.example.project.repo.UserRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference

    private val _favoriteUsers = MutableLiveData<List<UserEntity>>()
    val favoriteUsers: LiveData<List<UserEntity>> get() = _favoriteUsers

    private val _archivedUsers = MutableLiveData<List<UserEntity>>()
    val archivedUsers: LiveData<List<UserEntity>> get() = _archivedUsers

    private val _users = MutableLiveData<List<UserEntity>>()
    val users: LiveData<List<UserEntity>> get() = _users

    //var for ad
    private var interstitialAd: InterstitialAd? = null

    //private var nativeAd: NativeAd? = null

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://fake-json-api.mock.beeceptor.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Fetch all users from API and save them in Firestore
    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val apiResponseList = apiService.getUsers()
                Log.d("UserViewModel", "Fetched users: $apiResponseList")

                val userList = apiResponseList.map { apiResponse ->
                    UserEntity(
                        id = apiResponse.id.toLong(),
                        name = apiResponse.name,
                        email = apiResponse.email,
                        photoUrl = apiResponse.photo,
                        favorite = false,
                        archived = false
                    )
                }

                userList.forEach { user ->
                    try {
                        insertUser(user)
                    } catch (e: Exception) {
                        Log.e("UserViewModel", "Error inserting user ${user.name}: ${e.message}")
                    }
                }

                _users.value = userList

            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching users: ${e.message}")
            }
        }
    }

    //Insert fun
    fun insertUser(user: UserEntity) {
        viewModelScope.launch {
            repository.insertUser(user)
        }
    }

    fun toggleFavorite(user: UserEntity) {
        viewModelScope.launch {
            val updatedUser = user.copy(favorite = !user.favorite)
            Log.d("UserViewModel", "Toggling favorite for user: ${user.name}, New isFavorite: ${updatedUser.favorite}")
            repository.updateUser(updatedUser)
        }
    }

    // Initialize favorite users live data
    fun initFavoriteUsers() {
        Log.d("UserViewModel", "initFavoriteUsers called")
        repository.getFavoriteUsersLiveData().observeForever { favoriteUsersList ->
            Log.d("UserViewModel", "Favorite users updated: $favoriteUsersList")
            _favoriteUsers.postValue(favoriteUsersList)
        }
    }

    fun archiveUser(user: UserEntity) {
        viewModelScope.launch {
            val updatedUser = user.copy(archived = !user.archived)
            repository.updateUser(updatedUser)
        }
    }

    fun initArchivedUsers() {
        Log.d("UserViewModel", "initArchivedUsers called")
        repository.getArchivedUsersLiveData().observeForever { archivedUsers ->
            Log.d("UserViewModel", "Archived users updated: $archivedUsers")
            _archivedUsers.postValue(archivedUsers)
        }
    }

    fun renameUser(user: UserEntity, newName: String) {
        viewModelScope.launch {
            repository.updateUser(user.copy(name = newName))
        }
    }

    // initialize the Firestore listener for user updates
    fun initFirestoreListener() {
        repository.getAllUsersLiveData().observeForever { userList ->
            _users.postValue(userList)
        }
    }

    // Function to update user profile picture
    fun updateUserProfilePicture(context: Context, user: UserEntity, photoUrl: Uri, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("UserViewModel", "Profile picture $photoUrl")
                // Specify the path where the image will be stored in Firebase Storage
                val imageRef = storageRef.child("images/${user.id}/fileName.png")
                Log.d("UserViewModel", "Profile picture $imageRef")

                // Upload the image to Firebase Storage
                imageRef.putFile(photoUrl).await()

                // Get the download URL
                val downloadUri = imageRef.downloadUrl.await()

                // Create a new user object with the updated photo URL
                val updatedUser = user.copy(photoUrl = downloadUri.toString())

                // Call the repository to update the user in Firestore
                repository.updateUser(updatedUser)

                // Log success if needed
                Log.d("UserViewModel", "Profile picture updated successfully.")
                callback(true) // Notify success

            } catch (e: Exception) {
                // Handle any exceptions
                Log.e("UserViewModel", "Error uploading image: ${e.message}")
                callback(false) // Notify failure
            }
        }
    }

    // Function to load the interstitial ad
    fun loadInterstitialAd(context: Context) {
        Log.d("UserViewModel", "Loading interstitial ad")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("UserViewModel", "Ad loaded successfully")
                    interstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdClicked() {
                            // Called when a click is recorded for an ad.
                            Log.d(TAG, "Ad was clicked.")

                        }

                        override fun onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            Log.d(TAG, "Ad dismissed fullscreen content.")
                            interstitialAd = null
                            loadInterstitialAd(context)
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            // Called when ad fails to show.
                            Log.e(TAG, "Ad failed to show fullscreen content.")
                            interstitialAd = null
                        }

                        override fun onAdImpression() {
                            // Called when an impression is recorded for an ad.
                            Log.d(TAG, "Ad recorded an impression.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            Log.d(TAG, "Ad showed fullscreen content.")
                        }
                    }
                }

                override fun onAdFailedToLoad(adLoadError: LoadAdError) {
                    interstitialAd = null
                    Log.d("UserViewModel", "Ad failed to load: ${adLoadError.message}")
                }
            }
        )
    }

    // Function to show the interstitial ad
    fun showInterstitialAd(activity: Activity, onAdShown: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            Log.d("UserViewModel", "Interstitial ad displayed.")
            onAdShown() // Notify that the ad was shown

            // Reset the ad to force loading a new one for the next click
            interstitialAd = null
        } else {
            Log.d("UserViewModel", "The interstitial ad wasn't ready yet. Attempting to load a new ad.")
        }
    }
//    fun loadNativeAd(activity: Activity, nativeAdView: NativeAdView) {
//        val adLoader = AdLoader.Builder(activity, "YOUR_NATIVE_AD_UNIT_ID")
//            .forNativeAd { nativeAd ->
//                // Populate the native ad into the NativeAdView
//                populateNativeAdView(nativeAd, nativeAdView)
//            }
//            .withAdListener(object : AdListener() {
//                override fun onAdFailedToLoad(adError: LoadAdError) {
//                    Log.e("UserViewModel", "Native ad failed to load: ${adError.message}")
//                    nativeAdView.visibility = View.GONE // Hide native ad view if failed
//                }
//            })
//            .build()
//
//        // Load the native ad
//        adLoader.loadAd(AdRequest.Builder().build())
//    }
//
//
//    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
//        adView.findViewById<TextView>(R.id.native_ad_title).text = nativeAd.headline
//        adView.findViewById<TextView>(R.id.native_ad_body).text = nativeAd.body
//        adView.findViewById<Button>(R.id.native_ad_call_to_action).text = nativeAd.callToAction
//
//        // Load the image if available
//        if (nativeAd.images.isNotEmpty()) {
//            val imageView = adView.findViewById<ImageView>(R.id.native_ad_image)
//            val adImage = nativeAd.images[0].drawable
//            imageView.setImageDrawable(adImage)
//        }
//
//        // Add the native ad view to your layout
//        adView.setNativeAd(nativeAd)
//    }
}