package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val appService: ApiService,
) : ViewModel() {
//class SignUpViewModel() : ViewModel() {
    //private val dependencyContainer = DependencyContainer.getInstance()
    //private val appService = dependencyContainer.apiService
    //private val appAuth = dependencyContainer.appAuth

    val data = appAuth.authState
        .asLiveData() // приведём к LiveData

    val isAuthorized: Boolean
        get() = appAuth.authState.value != null

    fun registerUser(login: String, pass: String, name: String) {

        val vms = viewModelScope.launch {
            try {
                val userName = name;
                val password = pass;
                val userLogin = login;

                val responseToken: Response<Token> =
                    appService.registerUser(userLogin, password, userName)
                val token: Token = responseToken.body() ?: throw ApiError(
                    responseToken.code(),
                    responseToken.message()
                )
                // Сохраните указанную пару (id, token) в AppAuth
                appAuth.setAuth(token)

            } catch (e: IOException) {
                throw NetworkError
            } catch (e: Exception) {
                //throw UnknownError
            }
        }

    }
}