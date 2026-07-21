package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ZipFileEntity::class], version = 1, exportSchema = false)
abstract class ZipDatabase : RoomDatabase() {
    abstract fun zipDao(): ZipDao

    companion object {
        @Volatile
        private var INSTANCE: ZipDatabase? = null

        fun getDatabase(context: Context): ZipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZipDatabase::class.java,
                    "zip_vault_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
