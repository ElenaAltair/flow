package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PostRemoteKeyEntity(
    // KeyType - это свойство будет являться первичным ключом для таблицы PostRemoteKeyEntity
    @PrimaryKey
    val type: KeyType,
    val id: Long, // - id поста
) {
    // у нас будет только два типа данных:
    // пост, который находится на самом верху и пост в самом низу
    enum class KeyType {
        AFTER,
        BEFORE,
    }
}