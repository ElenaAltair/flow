package ru.netology.nmedia.repository

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.model.PhotoModel
import java.io.File
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll()
        .map(List<PostEntity>::toDto) // преобразуем List<PostEntity> в List<Post>
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = Api.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun thereAreNewPosts(): Boolean {
        try {
            return if (dao.maxId() > dao.maxVisibleId()) {
                true
            } else false
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getAllNew() {
        try {
            dao.updateShow(dao.maxId())
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = Api.service.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity().map { it.copy(show = 0) }) // <---
            emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)


    override suspend fun save(post: Post) {
        try {
            val response = Api.service.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, photoModel: PhotoModel) {
        try {
            // сначало отправляем media
            val media = uploade(photoModel.file)

            val response = Api.service.save(
                post.copy(
                    attachment = Attachment(
                        url = media.id,
                        AttachmentType.IMAGE
                    )
                )
            )
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private suspend fun uploade(file: File): Media {
        try {
            val response = Api.service.upload(
                MultipartBody.Part.createFormData(
                    "file", // "file" - ключ, точно такой же какой ожидает сервер
                    file.name, // имя файла может быть любым или отсутствовать
                    file.asRequestBody(),
                )
            )
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())

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
            val response = Api.service.removeById(id)
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
            //dao.updateShow(idMaxOld)

            // делаем запрос на изменение лайка поста на сервере
            val response: Response<Post> = if (!postFindByIdOld.likedByMe) {
                Api.service.likeById(id)
            } else {
                Api.service.dislikeById(id)
            }
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
                //dao.updateShow(idMaxOld)
                throw ApiError(response.code(), response.message())
            }

            // в качетве тела запроса нам возвращается Post
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            // сохраняем пост в базе данных
            dao.insert(PostEntity.fromDto(body)) // PostEntity.fromDto(body) - преобразуем Post в PostEntity
            //dao.updateShow(idMaxOld)
        } catch (e: IOException) {
            dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            //dao.updateShow(idMaxOld)
            throw NetworkError
        } catch (e: Exception) {
            dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            //dao.updateShow(idMaxOld)
            throw UnknownError
        }
    }


}
