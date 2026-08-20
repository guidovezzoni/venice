package com.guidovezzoni.venice.di

import com.guidovezzoni.venice.core.analytics.AnalyticsClient
import com.guidovezzoni.venice.core.analytics.AnalyticsProvider
import com.guidovezzoni.venice.core.analytics.CompositeAnalyticsClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsModule {

    @Binds
    @Singleton
    fun bindAnalyticsClient(
        implementation: CompositeAnalyticsClient,
    ): AnalyticsClient

    @Multibinds
    fun bindAnalyticsProviderSet(): Set<AnalyticsProvider>
}
