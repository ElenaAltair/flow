package ru.netology.nmedia.repository

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import retrofit2.Response
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException

var idMaxOld: Long = 0

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll()
        .map(List<PostEntity>::toDto) // преобразуем List<PostEntity> в List<Post>
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
            dao.updateShow(idMaxOld)

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun thereAreNewPosts(): Boolean {
        try {
            return if (dao.maxId() > idMaxOld) {
                true
            } else false
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getAllNew() {
        try {
            // Определим текущий максимальный id в базе данных
            idMaxOld = dao.maxId()
            dao.updateShow(idMaxOld)

        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = PostsApi.service.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())


            dao.insert(body.toEntity())
            dao.updateShow(idMaxOld)
            println("idMaxOld $idMaxOld ")
            emit(body.size)


        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.service.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
            dao.updateShow(idMaxOld)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            // удаляем пост в базе данных
            dao.removeById(id)

            // делаем запрос на удаление поста на сервере
            val response = PostsApi.service.removeById(id)
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                throw ApiError(response.code(), response.message())
            }


        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        var postFindByIdOld = dao.findById(id)
        try {
            // сохраняем пост в базе данных
            val postFindByIdNew = postFindByIdOld.copy(
                likedByMe = !postFindByIdOld.likedByMe,
                likes = postFindByIdOld.likes + if (postFindByIdOld.likedByMe) -1 else 1
            )
            dao.insert(postFindByIdNew)
            dao.updateShow(idMaxOld)

            // делаем запрос на изменение лайка поста на сервере
            val response: Response<Post> = if (!postFindByIdOld.likedByMe) {
                PostsApi.service.likeById(id)
            } else {
                PostsApi.service.dislikeById(id)
            }
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
                dao.updateShow(idMaxOld)
                throw ApiError(response.code(), response.message())
            }

            // в качетве тела запроса нам возвращается Post
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            // сохраняем пост в базе данных
            dao.insert(PostEntity.fromDto(body)) // PostEntity.fromDto(body) - преобразуем Post в PostEntity
            dao.updateShow(idMaxOld)
        } catch (e: IOException) {
            dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            dao.updateShow(idMaxOld)
            throw NetworkError
        } catch (e: Exception) {
            dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            dao.updateShow(idMaxOld)
            throw UnknownError
        }
    }

}
