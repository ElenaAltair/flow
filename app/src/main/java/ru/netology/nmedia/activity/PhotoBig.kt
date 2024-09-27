package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentPhotoBigBinding
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.view.load
import ru.netology.nmedia.viewmodel.PostViewModel

class PhotoBig : Fragment() {

    companion object {
        var Bundle.textUrl: String? by StringArg
    }

    //private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPhotoBigBinding.inflate(
            inflater,
            container,
            false
        )

        binding.bigPhoto.load("${BuildConfig.BASE_URL}/media/${arguments?.textUrl}")

        return binding.root
    }


}