package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyDao
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.model.PhotoModel
import java.io.File
import java.io.IOException
import javax.inject.Inject

// научим Dagger Hilt создавать объект типа PostRepository, так чтобя его реализация была PostRepositoryImpl @Inject constructor

class PostRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val apiService: ApiService, // Api - класс для доступа к сети
    postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb,
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = false),
        pagingSourceFactory = { postDao.pagingSource() },
        remoteMediator = PostRemoteMediator(
            service = apiService,
            postDao = postDao,
            postRemoteKeyDao = postRemoteKeyDao,
            appDb = appDb,
            )
    ).flow
        .map { it.map(PostEntity::toDto) } // преобразуем PostEntity к Post

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun thereAreNewPosts(): Boolean {
        try {
            return postDao.maxId() > postDao.maxVisibleId()
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getAllNew() {
        try {
            postDao.updateShow(postDao.maxId())
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity().map { it.copy(show = 0) }) // <---
            emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)


    override suspend fun save(post: Post) {
        try {
            val response = apiService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))

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

            val response = apiService.save(
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
            postDao.insert(PostEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private suspend fun uploade(file: File): Media {
        try {
            val response = apiService.upload(
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
            postDao.removeById(id)

            // делаем запрос на удаление поста на сервере
            val response = apiService.removeById(id)
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
        var postFindByIdOld = postDao.findById(id)
        try {
            // сохраняем пост в базе данных
            val postFindByIdNew = postFindByIdOld.copy(
                likedByMe = !postFindByIdOld.likedByMe,
                likes = postFindByIdOld.likes + if (postFindByIdOld.likedByMe) -1 else 1
            )
            postDao.insert(postFindByIdNew)
            //dao.updateShow(idMaxOld)

            // делаем запрос на изменение лайка поста на сервере
            val response: Response<Post> = if (!postFindByIdOld.likedByMe) {
                apiService.likeById(id)
            } else {
                apiService.dislikeById(id)
            }
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                postDao.insert(postFindByIdOld) // вернём базу данных к исходному виду
                //dao.updateShow(idMaxOld)
                throw ApiError(response.code(), response.message())
            }

            // в качетве тела запроса нам возвращается Post
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            // сохраняем пост в базе данных
            postDao.insert(PostEntity.fromDto(body)) // PostEntity.fromDto(body) - преобразуем Post в PostEntity
            //dao.updateShow(idMaxOld)
        } catch (e: IOException) {
            postDao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            //dao.updateShow(idMaxOld)
            throw NetworkError
        } catch (e: Exception) {
            postDao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            //dao.updateShow(idMaxOld)
            throw UnknownError
        }
    }


}
