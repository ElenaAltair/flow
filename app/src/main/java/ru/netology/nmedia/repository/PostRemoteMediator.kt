package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val service: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb, // для использования транзакции передадим экземпляр базы данных в конструктор
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {

        try {
            val response = when (loadType) {
                // пользователь хочет обновить список
                // state.config.pageSize - размер страницы

                // Измените код из лекции так, чтобы:
                // REFRESH не затирал предыдущий кеш, а добавлял данные сверху,
                // учитывая ID последнего поста сверху.
                // Соответственно, swipe to refresh должен добавлять данные, а не затирать их.
                LoadType.REFRESH -> {

                    val id = postRemoteKeyDao.max() ?: 0L

                    if(id > 0L){
                        service.getAfter(id, state.config.pageSize)
                    } else {
                        service.getLatest(state.config.pageSize)
                    }

                }


                // пользователь скролит наверх (запрос на получение верхней страницы)
                // state.firstItemOrNull()?.id - id самого первого элемента в отображённом списке

                // Измените код из лекции так, чтобы:
                // Автоматический PREPEND был отключен,
                // т. е. при scroll к первому сверху элементу данные автоматически не подгружались
                LoadType.PREPEND -> {
                    //val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(false)
                    //service.getAfter(id,  state.config.pageSize)
                    return MediatorResult.Success(false)
                }
                // пользователь скролит вниз (запрос на получение нижней страницы)
                // state.lastItemOrNull()?.id - id самого последнего элемента в отображённом списке
                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    service.getBefore(id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message(),
            )

            appDb.withTransaction {
                // заполняем таблицу ключей в базе данных, данными, которые приходят по сети
                when (loadType) {
                    // Измените код из лекции так, чтобы:
                    // REFRESH не затирал предыдущий кеш, а добавлял данные сверху,
                    // учитывая ID последнего поста сверху.
                    // Соответственно, swipe to refresh должен добавлять данные, а не затирать их.
                    LoadType.REFRESH -> {
                        //postDao.clear()

                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER, // самый первый пост из пришедшего списка
                                    body.first().id, // id первого поста
                                ),
                                /*
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE, // самый последний пост из пришедшего списка
                                    body.last().id, // id последнего поста
                                ),
                                */
                            )
                        )
                    }

                    // Измените код из лекции так, чтобы:
                    // Автоматический PREPEND был отключен,
                    // т. е. при scroll к первому сверху элементу данные автоматически не подгружались
                    LoadType.PREPEND -> { // скролл наверх
                        /*
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER, // самый первый пост из пришедшего списка
                                    body.first().id, // id первого поста
                                ),
                            )
                        )
                        */
                    }

                    LoadType.APPEND -> { // скролл вниз
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE, // самый последний пост из пришедшего списка
                                    body.last().id, // id последнего поста
                                ),
                            )
                        )
                    }
                }

                // записывем в базу список пришедших элементов, незабывая преобразовать его из Post в PostEntity
                postDao.insert(body.map(PostEntity::fromDto))
            }

            // MediatorResult.Success - у данного метода только один аргумент,
            // который проверяет достигли ли мы конца страницы
            return MediatorResult.Success(body.isEmpty())
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}