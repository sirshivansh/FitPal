package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_photos")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val note: String,
    val date: String // "yyyy-MM-dd"
)
