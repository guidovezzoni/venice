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
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsClient(
        implementation: CompositeAnalyticsClient,
    ): AnalyticsClient

    @Multibinds
    abstract fun bindAnalyticsProviderSet(): Set<AnalyticsProvider>
}
