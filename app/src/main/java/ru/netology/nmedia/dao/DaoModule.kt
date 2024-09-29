package ru.netology.nmedia.dao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostRemoteKeyDao

@InstallIn(SingletonComponent::class)
@Module
object DaoModule {

    @Provides
    fun providePostRemoteKeyDao(db: AppDb): PostRemoteKeyDao = db.postRemoteKeyDao()
}