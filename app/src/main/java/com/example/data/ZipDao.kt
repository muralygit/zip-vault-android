package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZipDao {
    @Query("SELECT * FROM zip_files ORDER BY dateAdded DESC")
    fun getAllZipFiles(): Flow<List<ZipFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZipFile(zipFile: ZipFileEntity): Long

    @Update
    suspend fun updateZipFile(zipFile: ZipFileEntity)

    @Query("DELETE FROM zip_files WHERE id = :id")
    suspend fun deleteZipFileById(id: Int)

    @Query("SELECT * FROM zip_files WHERE id = :id")
    suspend fun getZipFileById(id: Int): ZipFileEntity?

    @Query("SELECT * FROM zip_files WHERE name = :name AND size = :size LIMIT 1")
    suspend fun findByNameAndSize(name: String, size: Long): ZipFileEntity?
}
