package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zip_files")
data class ZipFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val filePath: String,
    val size: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val tags: String = "",
    val note: String = ""
)
