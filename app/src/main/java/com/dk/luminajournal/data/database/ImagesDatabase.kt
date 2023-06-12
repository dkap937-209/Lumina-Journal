package com.dk.luminajournal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ImagesToUploadDao::class],
    version = 1,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImagesToUploadDao
}