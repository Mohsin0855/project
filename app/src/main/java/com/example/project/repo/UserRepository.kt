package com.example.project.repo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.project.db.UserEntity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    //private val newUser:DatabaseReference = FirebaseDatabase.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    //collection for db ref for favrt
    private val userCollection = database.child("users")
    //private val database: DatabaseReference = Firebase.database.reference
    // Fetch users in real-time using LiveData
    fun getAllUsersLiveData(): LiveData<List<UserEntity>> {
        val liveData = MutableLiveData<List<UserEntity>>()

        usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {

                Log.e("UserRepository", "Error fetching users: ${error.message}")
                liveData.value = emptyList()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val users = snapshot.toObjects(UserEntity::class.java)
                liveData.value = users
            } else {
                liveData.value = emptyList()
            }
        }

        return liveData
    }

    // Fetch favorite users using a real-time listener
    fun getFavoriteUsersLiveData(): LiveData<List<UserEntity>> {
        val liveData = MutableLiveData<List<UserEntity>>()

        usersCollection.whereEqualTo("favorite", true).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserRepository", "Error fetching favorite users: ${error.message}")
                liveData.value = emptyList()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val favoriteUsers = snapshot.toObjects(UserEntity::class.java)
                Log.e("UserRepository", "favoriteUsers: ${favoriteUsers.size}")
                liveData.value = favoriteUsers
            } else {
                liveData.value = emptyList()
            }
        }

        return liveData
    }


    fun getArchivedUsersLiveData(): LiveData<List<UserEntity>> {
        val liveData = MutableLiveData<List<UserEntity>>()

        usersCollection.whereEqualTo("archived", true).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserRepository", "Error fetching favorite users: ${error.message}")
                liveData.value = emptyList()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val favoriteUsers = snapshot.toObjects(UserEntity::class.java)
                Log.e("UserRepository", "favoriteUsers: ${favoriteUsers.size}")
                liveData.value = favoriteUsers
            } else {
                liveData.value = emptyList()
            }
        }

        return liveData
    }

    // Insert
    suspend fun insertUser(user: UserEntity) {
        try {
            usersCollection.document(user.id.toString()).set(user).await()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error inserting user: ${e.message}")
        }
    }

    suspend fun updateUser(user: UserEntity) {
        try {
            usersCollection.document(user.id.toString()).set(user).await()
            Log.d("FirestoreUpdate", "User updated successfully: $user") // Log user data
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error updating user: ${e.message}")
        }
    }
}
