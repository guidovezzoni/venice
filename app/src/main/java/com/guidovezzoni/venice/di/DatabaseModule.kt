package com.guidovezzoni.venice.di

import android.content.Context
import androidx.room.Room
import com.guidovezzoni.venice.data.database.AppDatabase
import com.guidovezzoni.venice.data.database.dao.TripDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()

    private const val DATABASE_NAME = "venice_database"
}
