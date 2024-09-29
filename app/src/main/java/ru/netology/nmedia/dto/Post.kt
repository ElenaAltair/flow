package ru.netology.nmedia.dto

sealed class FeedItem {
    abstract val id: Long
}

data class Post(
    override val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    var attachment: Attachment? = null,
    val ownedByMe: Boolean = false, // являюсь ли я автором или нет
) : FeedItem()

// реклама
data class Ad(
    override val id: Long,
    val image: String,
) : FeedItem()

