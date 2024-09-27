package ru.netology.nmedia.activity

import com.google.android.gms.common.GoogleApiAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleApiAvailabilityModule {

    @Singleton
    @Provides
    fun providerGoogleApiAvailability(): GoogleApiAvailability{
        return GoogleApiAvailability()
    }
}