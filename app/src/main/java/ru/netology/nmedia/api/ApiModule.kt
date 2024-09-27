package ru.netology.nmedia.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Singleton

// Модуль для сетевого взаимодействия
@InstallIn(SingletonComponent::class)
@Module
class ApiModule {

    companion object {
        // адрес нашего сервера
        private const val BASE_URL = "${BuildConfig.BASE_URL}/api/slow/"
    }

    // создадим объект для логирования
    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Singleton
    @Provides
    fun provideOkHttp(
        logging: HttpLoggingInterceptor, // на входе на потребуется логирование
        appAuth: AppAuth // а также класс, который работает с авторизацией
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request =
                appAuth.authState.value?.token?.let { // в случае успеха подставим в каждый запрос заголовка Authorization
                    chain.request().newBuilder()
                        .addHeader("Authorization", it)
                        .build()
                } ?: chain.request() // если ничего не получилось, возьмём исходный запрос

            chain.proceed(request) // функция proceed() принимает запрос и возвращает ответ
        }
        .addInterceptor(logging)
        .build()

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    fun provideApiService(
        retrofit: Retrofit,
    ): ApiService = retrofit.create()

}