package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token

// Класс отвечающий за авторизацию
// Создан приватный конструктор, чтобы нельзя было создавать этот объект из других мест
class AppAuth private constructor(context: Context) {

    // преференсы
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    // здесь будем хранить токены
    private val _authState = MutableStateFlow<Token?>(null)
    val authState: StateFlow<Token?> = _authState.asStateFlow()


    init {
        val id = prefs.getLong(ID_KEY, 0L)
        val token = prefs.getString(TOKEN_KEY, null)

        if (id == 0L || token == null) {
            prefs.edit { clear() } // в этом случае очистить преферансы
        } else {
            // сразу объкты в преферансы сохранять нельзя, поэтому дествуем таким образом
            _authState.value = Token(id = id, token = token)
        }

        sendPushToken()
    }

    // функция запомнить аутентификацию
    @Synchronized
    fun setAuth(token: Token) {
        // отредакируем префы
        prefs.edit {
            putLong(ID_KEY, token.id)
            putString(TOKEN_KEY, token.token)
        }
        // теперь обновим _authState
        _authState.value = token

        sendPushToken()
    }

    // функция очистить аутентификацию
    @Synchronized
    fun clear() {
        // очистим префы
        prefs.edit { clear() }
        // теперь обновим _authState
        _authState.value = null

        sendPushToken()
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // формируем PushToken
                val pushToken = PushToken(token ?: Firebase.messaging.token.await())

                // отправляем PushToken на сервер
                Api.service.save(pushToken)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val TOKEN_KEY = "TOKEN_KEY"
        const val ID_KEY = "ID_KEY"

        @Volatile
        private var INSTANCE: AppAuth? = null

        fun getInstance(): AppAuth =
            requireNotNull(INSTANCE) { // возвращаем INSTANCE, если он существует
                "You should call initApp(context) first"
            }

        fun initApp(context: Context) {
            INSTANCE = AppAuth(context.applicationContext)
        }
    }
}