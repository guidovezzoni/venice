package com.guidovezzoni.venice.di

import com.guidovezzoni.venice.core.analytics.AnalyticsProvider
import com.guidovezzoni.venice.core.analytics.DebugAnalyticsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugAnalyticsModule {

    @Binds
    @IntoSet
    abstract fun bindDebugAnalyticsProvider(
        implementation: DebugAnalyticsProvider,
    ): AnalyticsProvider
}
