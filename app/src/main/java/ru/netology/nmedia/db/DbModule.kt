package ru.netology.nmedia.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.dao.PostDao
import javax.inject.Singleton


//Для предоставления зависимостей в Dagger Hilt используется такое понятие, как Модули - @Module
// Также укажем на каком уровне будет использоваться наша зависимость
// (в нашем случае на уровне всего приложения,
// так как база данных у нас глобальна для всего приложения) - @InstallIn(SingletonComponent::class)
@InstallIn(SingletonComponent::class)
@Module
class DbModule {

    // Если мы хотим в ручную создать экземпляр какого-то объекта,
    // то его надо пометить аннотацией @Provides

    // Пометим функции аннотациями, говорящими, сколько живет данный объект -
    // если надо чтобы объект жил в рамках всего приложения в единственном экземпляре,
    // используем аннотацию @Singleton

    // Функция создания базы данных
    @Singleton
    @Provides
    fun provideDb(
        @ApplicationContext
        context: Context //передаём контекст приложения
    ): AppDb = Room.databaseBuilder(context, AppDb::class.java, "app.db")
        //  .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun providePostDao(
        appDb: AppDb
    ): PostDao = appDb.postDao()
}