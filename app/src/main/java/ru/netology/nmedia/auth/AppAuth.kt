package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }

    // функция очистить аутентификацию
    @Synchronized
    fun clear() {
        // очистим префы
        prefs.edit { clear() }
        // теперь обновим _authState
        _authState.value = null
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