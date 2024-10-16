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

class FavouriteFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedUser: UserEntity? = null // Store the user for which the image is selected
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var adView: AdView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the AdView
        adView = view.findViewById(R.id.adView)

        // Load the adaptive banner ad
        loadBannerAd()

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
                                // Handle any error (e.g. show a toast message)
                            }
                        }
                    }
                }
            }
        }

        Log.d("fragment","favourite fragment")
        // Initialize the RecyclerView for favorites
        userAdapter = UserAdapter(
            items   = emptyList(),
            onFavoriteToggle = {
                    user -> userViewModel.toggleFavorite(user)
                Log.d("onFavoriteToggle","onFavoriteToggle")},

            //archiveUser = { user -> userViewModel.archiveUser(user) },
            archiveUser = { user -> userViewModel.archiveUser(user) },
            onRenameUser = { user, newName -> userViewModel.renameUser(user, newName) },
            onImageClick = { user ->
                selectedUser = user // Store the user for which the image is being updated
                openGallery()// Request permission and open gallery
            },
            onUserClick = { user ->
                // Show the interstitial ad when the user clicks an item
                userViewModel.showInterstitialAd(requireActivity()) {
                    Log.d("ArchiveFragment", "User clicked on: ${user.name}")
                }
            }

        )

        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }

        // Initialize favorite users from the ViewModel
        userViewModel.initFavoriteUsers()


        // Observe favorite users and update the adapter when they change
        userViewModel.favoriteUsers.observe(viewLifecycleOwner) { favoriteUsers ->
            loadingIndicator.visibility = View.GONE
            Log.d("FavouriteFragment", "Observed favorite users: $favoriteUsers")
            userAdapter.updateList(favoriteUsers)
            if (favoriteUsers.isEmpty()) {
                Toast.makeText(requireContext(), "No favorite users found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun openGallery() {

        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            Log.d("fun","message")
        }
        // Launch the gallery intent using the ActivityResultLauncher
        pickImageLauncher.launch(intent)
    }

    private fun loadBannerAd() {
        // Create and load the ad request
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onDestroyView() {
        // Clean up the AdView when the view is destroyed
        if (::adView.isInitialized) {
            adView.destroy()
        }
        super.onDestroyView()
    }
}
