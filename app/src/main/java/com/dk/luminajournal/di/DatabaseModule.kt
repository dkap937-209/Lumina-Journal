package com.dk.luminajournal.di

import android.content.Context
import androidx.room.Room
import com.dk.util.connectivity.NetworkConnectivityObserver
import com.dk.mongo.database.ImagesDatabase
import com.dk.util.Constants.IMAGES_DATABASE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ImagesDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = ImagesDatabase::class.java,
            name = IMAGES_DATABASE
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideImageToUploadDao(
        database: ImagesDatabase
    ) = database.imageToUploadDao()

    @Provides
    @Singleton
    fun provideImageToDeleteDao(
        database: ImagesDatabase
    ) = database.imageToDeleteDao()

    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        @ApplicationContext context: Context
    ) = NetworkConnectivityObserver(context = context)
}