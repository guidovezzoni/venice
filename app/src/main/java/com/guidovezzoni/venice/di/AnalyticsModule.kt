package com.guidovezzoni.venice.di

import com.guidovezzoni.venice.BuildConfig
import com.guidovezzoni.venice.core.analytics.AnalyticsClient
import com.guidovezzoni.venice.core.analytics.AnalyticsProvider
import com.guidovezzoni.venice.core.analytics.CompositeAnalyticsClient
import com.guidovezzoni.venice.core.analytics.FirebaseAnalyticsProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Binds
    @IntoSet
    fun bindFirebaseAnalyticsProvider(
        implementation: FirebaseAnalyticsProvider,
    ): AnalyticsProvider

    @Multibinds
    fun bindAnalyticsProviderSet(): Set<AnalyticsProvider>

    companion object {

        @Provides
        @Singleton
        fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG
    }
}
