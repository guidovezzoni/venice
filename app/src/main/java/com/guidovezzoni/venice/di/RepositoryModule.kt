package com.guidovezzoni.venice.di

import com.guidovezzoni.venice.data.repository.PlaceSearchRepositoryImpl
import com.guidovezzoni.venice.data.repository.RouteRepositoryImpl
import com.guidovezzoni.venice.data.repository.StopRepositoryImpl
import com.guidovezzoni.venice.data.repository.TripRepositoryImpl
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import com.guidovezzoni.venice.domain.repository.RouteRepository
import com.guidovezzoni.venice.domain.repository.StopRepository
import com.guidovezzoni.venice.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    fun bindStopRepository(impl: StopRepositoryImpl): StopRepository

    @Binds
    @Singleton
    fun bindPlaceSearchRepository(impl: PlaceSearchRepositoryImpl): PlaceSearchRepository

    @Binds
    @Singleton
    fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository
}
