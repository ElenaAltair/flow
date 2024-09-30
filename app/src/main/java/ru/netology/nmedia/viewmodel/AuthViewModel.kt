package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
) : ViewModel() {
//class AuthViewModel() : ViewModel() {
    //private val dependencyContainer = DependencyContainer.getInstance()
    //private val appAuth = dependencyContainer.appAuth

    val data = appAuth.authState
        .asLiveData() // приведём к LiveData

    val isAuthorized: Boolean
        get() = appAuth.authState.value != null // нужен get(), чтобы проверка шла при каждом обращении, а не только один раз при создании

}