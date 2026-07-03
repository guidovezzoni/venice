package com.guidovezzoni.venice.di

import com.guidovezzoni.venice.data.analytics.CompositeAnalyticsTracker
import com.guidovezzoni.venice.data.analytics.LogAnalyticsProvider
import com.guidovezzoni.venice.domain.analytics.AnalyticsProvider
import com.guidovezzoni.venice.domain.analytics.AnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        implementation: CompositeAnalyticsTracker,
    ): AnalyticsTracker

    @Binds
    @IntoSet
    abstract fun bindLogAnalyticsProvider(
        implementation: LogAnalyticsProvider,
    ): AnalyticsProvider
}
