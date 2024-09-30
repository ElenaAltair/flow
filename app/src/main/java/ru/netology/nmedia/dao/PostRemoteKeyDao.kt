package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.PostRemoteKeyEntity

// интерфейс для доступа к базе данных PostRemoteKeyEntity
@Dao
interface PostRemoteKeyDao {

    // функция получения максимального id из базы данных (id самого нового поста в базе данных)
    @Query("SELECT max(id) FROM PostRemoteKeyEntity")
    suspend fun max(): Long?

    // функция получения минимального id из базы данных (id самого старого поста в базе данных)
    @Query("SELECT min(id) FROM PostRemoteKeyEntity")
    suspend fun min(): Long?

    // функция записи в таблицу новых данных или обновления старых
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(postRemoteKeyEntity: PostRemoteKeyEntity)

    // функция записи списка из данных экземпляров
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(postRemoteKeyEntity: List<PostRemoteKeyEntity>)

    // функция по очистке таблицы
    @Query("DELETE FROM PostRemoteKeyEntity")
    suspend fun clear()

}