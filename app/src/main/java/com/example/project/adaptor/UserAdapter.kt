package com.example.project.adaptor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project.R
import com.example.project.db.UserEntity
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView


class UserAdapter(
    private var items: List<Any>,  // This list now contains both UserEntity and NativeAd
    private val onFavoriteToggle: (UserEntity) -> Unit,
    private val archiveUser: (UserEntity) -> Unit,
    private val onRenameUser: (UserEntity, String) -> Unit,
    private val onImageClick: (UserEntity) -> Unit,
    private val onUserClick: (UserEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_NATIVE_AD = 2
    }

    // ViewHolder for UserEntity
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.nameTextView)
        val email: TextView = view.findViewById(R.id.emailTextView)
        val photo: ImageView = view.findViewById(R.id.profileImageView)
        val moreMenu: ImageView = view.findViewById(R.id.favoriteButton)
    }

    // ViewHolder for NativeAd
    class NativeAdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nativeAdView: NativeAdView = view as NativeAdView
        val headline: TextView = nativeAdView.findViewById(R.id.native_ad_headline)
        val body: TextView = nativeAdView.findViewById(R.id.native_ad_body)
        val callToAction: Button = nativeAdView.findViewById(R.id.native_ad_call_to_action)
        val image: ImageView = nativeAdView.findViewById(R.id.native_ad_image)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is UserEntity -> VIEW_TYPE_USER
            is NativeAd -> VIEW_TYPE_NATIVE_AD
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_NATIVE_AD -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.native_ad_layout, parent, false)
                NativeAdViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_USER -> {
                val userHolder = holder as UserViewHolder
                val user = items[position] as UserEntity

                userHolder.name.text = user.name
                userHolder.email.text = user.email

                Glide.with(holder.itemView.context)
                    .load(user.photoUrl)
                    .into(userHolder.photo)

                userHolder.photo.setOnClickListener {
                    onImageClick(user) // Trigger the click event
                }

                // Update favorite button icon based on isFavorite status
                userHolder.moreMenu.setImageResource(
                    if (user.favorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
                )

                userHolder.moreMenu.setOnClickListener {
                    onFavoriteToggle(user)
                }

                userHolder.moreMenu.setOnLongClickListener { view ->
                    val popupMenu = PopupMenu(view.context, view)
                    popupMenu.inflate(R.menu.menu_user_options)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_rename -> {
                                showRenameDialog(holder.itemView.context, user)
                                true
                            }
                            R.id.action_archive -> {
                                archiveUser(user)
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                    true
                }
                holder.itemView.setOnClickListener {
                    onUserClick(user) // Handle the click and show interstitial ad
                }
            }

            VIEW_TYPE_NATIVE_AD -> {
                val nativeAdHolder = holder as NativeAdViewHolder
                val nativeAd = items[position] as NativeAd

                // Set native ad properties
                nativeAdHolder.headline.text = nativeAd.headline
                nativeAdHolder.body.text = nativeAd.body
                nativeAdHolder.callToAction.text = nativeAd.callToAction

                // Load the ad image if available
                nativeAd.images.firstOrNull()?.let { image ->
                    nativeAdHolder.image.setImageDrawable(image.drawable)
                }

                // Assign the NativeAdView to the parent view
                nativeAdHolder.nativeAdView.setNativeAd(nativeAd)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun showRenameDialog(context: Context, user: UserEntity) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename_user, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextNewName)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Rename User")
            .setView(dialogView)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty()) {
                    onRenameUser(user, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}

