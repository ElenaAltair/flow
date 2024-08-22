package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity WHERE show = 1 ORDER BY id DESC") //
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) == 0 FROM PostEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT COUNT(*) FROM PostEntity")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("SELECT * FROM PostEntity WHERE id = :id ")
    suspend fun findById(id: Long): PostEntity

    @Query("SELECT max(id) FROM PostEntity ")
    suspend fun maxId(): Long

    @Query("SELECT max(id) FROM PostEntity WHERE show = 1")
    suspend fun maxVisibleId(): Long

    @Query("UPDATE PostEntity SET show = 1 WHERE id <= :id ")
    suspend fun updateShow(id: Long)
}
