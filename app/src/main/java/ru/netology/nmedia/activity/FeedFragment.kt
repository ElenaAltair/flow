package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.PhotoBig.Companion.textUrl
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        //createDialogSignOut()

        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                if (viewModel.isAuthorized) {
                    viewModel.likeById(post.id)
                } else {
                    createDialogSignIn()
                    alertDialog?.show()
                }
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onImagePhoto(post: Post) {
                // передадим url изображения выбранного поста
                // из фрагмента feedFragment во фрагмент photoBig
                findNavController().navigate(
                    R.id.action_feedFragment_to_photoBig,
                    Bundle().apply {
                        textUrl = post.attachment?.url
                    }
                )
            }

        })
        binding.list.adapter = adapter
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            binding.exFab.isVisible = state.thereAreNewPosts
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
        }
        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.emptyText.isVisible = state.empty


        }

        // подписка на появление новых постов на сервере
        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
            // TODO: just log it, interaction must be in homework
            println(state)
            viewModel.flagNewPosts()
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.fab.setOnClickListener {
            if (viewModel.isAuthorized) {
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            } else {
                createDialogSignIn()
                alertDialog?.show()
            }
        }

        binding.exFab.setOnClickListener {
            viewModel.loadNewPosts()
            binding.list.postDelayed({ binding.list.smoothScrollToPosition(0) }, 500)
        }

        return binding.root
    }

    fun createDialogSignIn() {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Log in")
        alertDialogBuilder.setMessage("To put down likes and add posts, you need to log in. Sign in?")
        alertDialogBuilder.setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                findNavController().navigate(
                    R.id.action_feedFragment_to_signInFragment
                )
        }
        alertDialogBuilder.setNegativeButton("Cancel", { dialogInterface: DialogInterface, i: Int -> })
        alertDialog = alertDialogBuilder.create()
    }

}
