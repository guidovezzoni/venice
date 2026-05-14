package com.guidovezzoni.venice.di

import com.guidovezzoni.venice.data.repository.StopRepositoryImpl
import com.guidovezzoni.venice.data.repository.TripRepositoryImpl
import com.guidovezzoni.venice.domain.repository.StopRepository
import com.guidovezzoni.venice.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    abstract fun bindStopRepository(impl: StopRepositoryImpl): StopRepository
}
