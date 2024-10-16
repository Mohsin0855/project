package com.example.project.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.adaptor.UserAdapter
import com.example.project.db.UserEntity
import com.example.project.viewmodel.UserViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAdView

class HomeFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter
    private lateinit var loadingIndicator: ProgressBar
    private var selectedUser: UserEntity? = null // Store the user for which the image is selected
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    // AdMob val
    private lateinit var adView: AdView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize and load the banner ad
        adView = view.findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        // Initialize the ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedUser?.let { user ->
                        loadingIndicator.visibility = View.VISIBLE // Show the loading indicator
                        userViewModel.updateUserProfilePicture(requireContext(), user, it) { success ->
                            loadingIndicator.visibility = View.GONE // Hide the loading indicator
                            if (!success) {
                                Log.d("msg", "$success")
                            }
                        }
                    }
                }
            }
        }

        // Initialize the UserAdapter
        userAdapter = UserAdapter(
            items = emptyList(),
            onFavoriteToggle = { user -> userViewModel.toggleFavorite(user) },
            archiveUser = { user -> userViewModel.archiveUser(user) },
            onRenameUser = { user, newName -> userViewModel.renameUser(user, newName) },
            onImageClick = { user ->
                selectedUser = user // Store the user for which the image is being updated
                openGallery() // Request permission and open gallery
            },
            onUserClick = { user ->
                // Show the interstitial ad when the user clicks an item
                userViewModel.showInterstitialAd(requireActivity()) {
                    Log.d("HomeFragment", "User clicked on: ${user.name}")
                }
            }
        )

        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }

        // Fetch users from the API
        userViewModel.fetchUsers()

        // Observe the users and load native ads along with them
        userViewModel.users.observe(viewLifecycleOwner) { users ->
            loadingIndicator.visibility = View.GONE
            loadNativeAdsAndUsers() // Call the function to load users and native ads
        }

        userViewModel.initFirestoreListener()
    }

    override fun onDestroyView() {
        // Clean up the AdView when the view is destroyed
        if (::adView.isInitialized) {
            adView.destroy()
        }
        super.onDestroyView()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    // Function to load users and native ads
    private fun loadNativeAdsAndUsers() {
        // Make sure the fragment is attached to an activity and pass the activity instead of context
        userViewModel.loadNativeAd(requireActivity()) { nativeAd ->
            val updatedList = mutableListOf<Any>()
Log.d("native","native ad $nativeAd")
            // Add users and native ads at intervals
            val users = userViewModel.users.value ?: emptyList()
            for (i in users.indices) {
                updatedList.add(users[i])

                // Add a native ad after every 5 users
                if (i % 5 == 0 && i != 0) {
                    updatedList.add(nativeAd)
                }
            }

            userAdapter.updateList(updatedList)
        }
    }
}
