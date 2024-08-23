package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArg: String? by StringArg
    }

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )

        arguments?.textArg
            ?.let(binding.edit::setText)

        val imagePikerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == ImagePicker.RESULT_ERROR){
                Toast.makeText(
                    requireContext(),
                    R.string.image_picker_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                val uri = it.data?.data ?: return@registerForActivityResult

                viewModel.updatePhoto(PhotoModel(uri = uri, file = uri.toFile()))
            }
        }

        binding.takePhoto.setOnClickListener{
            ImagePicker.with(this)
                .crop()
                .maxResultSize(2048, 2048)
                .cameraOnly() // только камера
                .createIntent (imagePikerLauncher::launch)
        }

        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo == null) {
                binding.photoContainer.isGone = true
                return@observe
            }

            binding.photoContainer.isVisible = true
            binding.photoPrewiew.setImageURI(photo.uri)
        }

        binding.removePhoto.setOnClickListener{
            viewModel.clearPhoto()
        }

        binding.gallery.setOnClickListener{
            ImagePicker.with(this)
                .crop()
                .maxResultSize(2048, 2048)
                .galleryOnly() // только галлерея
                .galleryMimeTypes(
                    arrayOf(
                        "image/png",
                        "image/jpeg",
                        "image/jpg",
                    )
                )
                .createIntent (imagePikerLauncher::launch)
        }
        
        requireActivity().addMenuProvider( // меню для окна редактирования
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.create_post_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    if (menuItem.itemId == R.id.save) {
                        viewModel.changeContent(binding.edit.text.toString())
                        viewModel.save()
                        AndroidUtils.hideKeyboard(requireView())
                        true
                    }else {
                        false
                    }

            },
            viewLifecycleOwner, // когда Lifecycle уничтожится, пункт меню удалится из активити
        )
        
        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }
        return binding.root
    }
}