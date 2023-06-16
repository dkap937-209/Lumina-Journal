package com.dk.luminajournal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dk.luminajournal.data.database.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class],
    version = 1,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImageToUploadDao
}