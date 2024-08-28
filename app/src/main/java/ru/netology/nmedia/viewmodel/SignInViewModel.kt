package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import java.io.IOException


class SignInViewModel : ViewModel() {

    val data = AppAuth.getInstance().authState
        .asLiveData() // приведём к LiveData

    val isAuthorized: Boolean
        get() = AppAuth.getInstance().authState.value != null

    fun userUpdate(login: String, pass: String) {

        val vms = viewModelScope.launch {
            try {
                val userName = login;
                val password = pass;

                val responseToken: Response<Token> = Api.service.updateUser(userName, password)
                val token: Token = responseToken.body() ?: throw ApiError(
                    responseToken.code(),
                    responseToken.message()
                )
                // Сохраните указанную пару (id, token) в AppAuth
                AppAuth.getInstance().setAuth(token)

            } catch (e: IOException) {
                throw NetworkError
            } catch (e: Exception) {
                //throw UnknownError
            }
        }

    }
}