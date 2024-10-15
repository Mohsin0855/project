package com.example.project.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserEntity")
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val email: String,
    val photoUrl: String,
    val favorite: Boolean = false,
    val archived: Boolean = false // New field for archived users
) {
    constructor() : this(0L, "", "", "", false, false) // Default values for Firestore

}