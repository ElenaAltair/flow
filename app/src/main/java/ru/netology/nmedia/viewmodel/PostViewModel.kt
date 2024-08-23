package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = "",
    //show = 1
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(context = application).postDao())

    val data: LiveData<FeedModel> = repository.data
        .map(::FeedModel)
        .catch {
            it.printStackTrace()
        }
        .asLiveData(Dispatchers.Default) // преобразуем flow в liveData

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _photo = MutableLiveData<PhotoModel?>(null) // по умолчанию ничего нет
    val photo : LiveData<PhotoModel?>
        get() = _photo

    // newerCount - количество новых постов, которые появились на сервере
    // switchMap позволяет нам пописаться на изменения data и на основании этого получить новую liveData
    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(
            it.posts.firstOrNull()?.id ?: 0L
        ) // мы запускаем flow c id последнего поста в нашей базе данных
            .catch { e -> e.printStackTrace() }
            .asLiveData(Dispatchers.Default) // преобразование flow в liveData
    }

    private val edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            val newPost = repository.thereAreNewPosts()
            _dataState.value = FeedModelState(thereAreNewPosts = newPost)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            val newPost = repository.thereAreNewPosts()
            _dataState.value = FeedModelState(thereAreNewPosts = newPost)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun loadNewPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(thereAreNewPosts = false)
            repository.getAllNew()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                try {

                    _photo.value?.let { photo ->
                        repository.saveWithAttachment(it, photo)
                    } ?: repository.save(it)

                    val newPost = repository.thereAreNewPosts()
                    _dataState.value = FeedModelState(thereAreNewPosts = newPost)
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id) // идём в class PostRepositoryImpl в функцию likeById(id) и сохраняем изменение лайка поста на сервере и базе
                val newPost = repository.thereAreNewPosts()
                _dataState.value = FeedModelState(thereAreNewPosts = newPost)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun flagNewPosts() {
        viewModelScope.launch {
            try {
                val newPost = repository.thereAreNewPosts()
                _dataState.value = FeedModelState(thereAreNewPosts = newPost)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id) // идём в class PostRepositoryImpl в функцию removeById(id) и удаляем пост на сервере и базе
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun clearPhoto() {
        _photo.value = null
    }

    fun updatePhoto(photoModel: PhotoModel) {
        _photo.value = photoModel
    }
}
