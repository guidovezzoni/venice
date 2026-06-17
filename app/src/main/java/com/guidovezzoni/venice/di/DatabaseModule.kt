package com.guidovezzoni.venice.di

import android.content.Context
import androidx.room.Room
import com.guidovezzoni.venice.data.database.AppDatabase
import com.guidovezzoni.venice.data.database.dao.LegDao
import com.guidovezzoni.venice.data.database.dao.StopDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()

    @Provides
    fun provideStopDao(db: AppDatabase): StopDao = db.stopDao()

    @Provides
    fun provideLegDao(db: AppDatabase): LegDao = db.legDao()

    private const val DATABASE_NAME = "venice_database"
}
