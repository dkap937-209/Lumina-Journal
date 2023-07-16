package com.dk.mongo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dk.mongo.database.entity.ImageToDelete
import com.dk.mongo.database.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImageToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}