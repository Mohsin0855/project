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
import com.google.android.gms.ads.AdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class ArchiveFragment : Fragment() {
    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter
    private var selectedUser: UserEntity? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var loadingIndicator: ProgressBar
    private var hasAdBeenShown = false // Flag to track if the ad has been shown

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        // Initialize the ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedUser?.let { user ->
                        loadingIndicator.visibility = View.VISIBLE // Show loading indicator
                        userViewModel.updateUserProfilePicture(requireContext(), user, it) { success ->
                            loadingIndicator.visibility = View.GONE // Hide loading indicator
                            if (!success) {
                                Toast.makeText(requireContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        userAdapter = UserAdapter(
            items = emptyList(),
            onFavoriteToggle = { user -> userViewModel.toggleFavorite(user) },
            archiveUser = { user ->
                Toast.makeText(requireContext(), "${user.name} is archived", Toast.LENGTH_SHORT).show()
                // Show ad when user is archived
            },
            onRenameUser = { user, newName -> userViewModel.renameUser(user, newName) },
            onImageClick = { user ->
                selectedUser = user // Store the user for image update
                openGallery() // Open gallery
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

        userViewModel.initArchivedUsers()
        userViewModel.archivedUsers.observe(viewLifecycleOwner) { archivedUsers ->
            loadingIndicator.visibility = View.GONE
            Log.d("ArchiveFragment", "Observed archived users: $archivedUsers")
            if (archivedUsers.isEmpty()) {
                Toast.makeText(requireContext(), "No archived users found", Toast.LENGTH_SHORT).show()
            }
            userAdapter.updateList(archivedUsers)
        }
    }

    override fun onResume() {
        super.onResume()
        // Only show the ad once per session
        if (!hasAdBeenShown) {
            userViewModel.showInterstitialAd(requireActivity()) {
                hasAdBeenShown = true // Mark ad as shown
                // Reload ad for future use
            }
        }else{hasAdBeenShown = false }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent) // Launch gallery
    }
}

