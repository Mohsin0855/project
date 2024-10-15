package com.example.project.adaptor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project.R
import com.example.project.db.UserEntity



class UserAdapter(
    private var users: List<UserEntity>,
    private val onFavoriteToggle: (UserEntity) -> Unit,
    private val archiveUser: (UserEntity) -> Unit,
    private val onRenameUser: (UserEntity, String) -> Unit,
    private val onImageClick: (UserEntity) -> Unit,
    private val onUserClick: (UserEntity) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.nameTextView)
        val email: TextView = view.findViewById(R.id.emailTextView)
        val photo: ImageView = view.findViewById(R.id.profileImageView)
        val moreMenu: ImageView = view.findViewById(R.id.favoriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.name.text = user.name
        holder.email.text = user.email

        Glide.with(holder.itemView.context)
            .load(user.photoUrl)
            .into(holder.photo)

        holder.photo.setOnClickListener {
            onImageClick(user) // Trigger the click event
        }

        // Update favorite button icon based on isFavorite status
        holder.moreMenu.setImageResource(
            if (user.favorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24
        )

        holder.moreMenu.setOnClickListener {
            onFavoriteToggle(user)
        }

        holder.moreMenu.setOnLongClickListener { view ->
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

    override fun getItemCount(): Int = users.size
    @SuppressLint("NotifyDataSetChanged")
    fun updateUsers(newUsers: List<UserEntity>) {
        Log.d("UserAdapter", "Updating users: $newUsers") // Log the new data
        users = newUsers
        notifyDataSetChanged()  // Always refresh the views for testing
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