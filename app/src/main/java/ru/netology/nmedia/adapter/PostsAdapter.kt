package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.view.load
import ru.netology.nmedia.view.loadCircleCrop

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun onImagePhoto(post: Post) {}
}

// Научим класс PostsAdapter работать не только с постами, но и с рекламой
// меняем тип Post на FeedItem
class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback()) {

    // получим тип элемента viewType
    override fun getItemViewType(position: Int): Int =
        // возьмём элемент из списка по позиции и определим его тип
        when (getItem(position)) {
            // если у нас id типа реклама, то мы вернем ссылку на макет с рекламой
            is Ad -> R.layout.card_ad
            // если это пост, то ссылку на макет поста
            is Post -> R.layout.card_post
            // мы не используем заглушки, поэтому элемент типа null недопустим
            null -> error("unknown item type")
        }


    // метод в котором мы создаем ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            // если тип это карточка поста, то создаем PostViewHolder
            R.layout.card_post -> {
                val binding =
                    CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }
            // если тип это реклама, то создаем AdViewHolder
            R.layout.card_ad -> {
                val binding =
                    CardAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }

            else -> error("unknown view type: $viewType")
        }


    // метод в котором мы заполняем ViewHolder новыми данными
    // необходимо сверстать элемент с рекламой и создать ViewHolder, для использования его в списке
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            // приведение типа
            // если это реклама, то мы приводим ViewHolder к AdViewHolder
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            null -> error("unknown item type")
        }
    }
}

// создадим класс ViewHolder для рекламы
class AdViewHolder(
    private val binding: CardAdBinding,
) : RecyclerView.ViewHolder(binding.root) {

    // создадим функцию bind, где будем заполнять карточку с рекламой
    fun bind(ad: Ad) {
        // для загрузки картинки, мы воспользуемся библиотекой Glade
        // путь до картинки с рекламой у нас будет построен по аналогии с аватарами
        binding.image.load("${BuildConfig.BASE_URL}/media/${ad.image}")

    }
}


class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            avatar.loadCircleCrop("${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}")
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            if (post.attachment != null && post.attachment?.url != null && post.attachment?.url != "") {
                imagePhoto.load("${BuildConfig.BASE_URL}/media/${post.attachment?.url}")
                imagePhoto.visibility = View.VISIBLE
            } else {
                imagePhoto.visibility = View.GONE
            }

            imagePhoto.setOnClickListener {
                onInteractionListener.onImagePhoto(post)
            }

            menu.isVisible = post.ownedByMe // меню видно только, если мы авторы поста

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        // при сравнениии элементов, добавим новую проверку,
        // чтобы не получить ситуацию, когда мы сравнили рекламу и пост, и у них совпали id
        // для этого проверим классы элементов, если они не совпадают, то элементы не равны
        if (oldItem::class != newItem::class) {
            return false
        }
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}
