package ru.netology.nmedia.activity

import com.google.android.gms.common.internal.Preconditions
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FirebaseMessagingModule {

    @Singleton
    @Provides
    fun providerFirebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging{
        return firebaseApp[FirebaseMessaging::class.java]
    }

    @Singleton
    @Provides
    fun providerFirebaseApp(): FirebaseApp{
        return FirebaseApp.getInstance()
    }


}