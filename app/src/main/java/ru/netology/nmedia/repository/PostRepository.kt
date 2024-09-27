package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.PhotoModel

interface PostRepository {
    //val data: Flow<List<Post>>
    val data: Flow<PagingData<Post>>
    suspend fun getAll()
    suspend fun getAllNew()
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun save(post: Post)
    suspend fun saveWithAttachment(post: Post, photoModel: PhotoModel)
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long)
    suspend fun thereAreNewPosts(): Boolean
}

