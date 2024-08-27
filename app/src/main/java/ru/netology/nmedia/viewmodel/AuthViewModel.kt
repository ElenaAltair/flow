package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import ru.netology.nmedia.auth.AppAuth

class AuthViewModel : ViewModel() {

    val data = AppAuth.getInstance().authState
        .asLiveData() // приведём к LiveData

    val isAuthorized: Boolean
        get() = AppAuth.getInstance().authState.value != null // нужен get(), чтобы проверка шла при каждом обращении, а не только один раз при создании

}