package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.SignInViewModel

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by viewModels()
    /*
    private val viewModel: SignInViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
    )
    */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentSignInBinding.inflate(
            inflater,
            container,
            false
        )


        binding.bSignIn.setOnClickListener {
            val userName = binding.login.text.toString().trim();
            val password = binding.pass.text.toString().trim();

            viewModel.userUpdate(userName, password)
        }

        binding.buttonSignUp.setOnClickListener {
            findNavController().navigate(
                R.id.action_signInFragment_to_signUpFragment
            )
        }

        viewModel.data.observe(viewLifecycleOwner) { state ->
            if (viewModel.isAuthorized) {
                binding.textMessage.text = "The user is logged in."
                findNavController().navigateUp() //закрыть текущий фрагмент и вернуться к предыдущему
            } else {
                binding.textMessage.text =
                    "The user is not logged in. Enter your username and password."
            }
        }

        return binding.root
    }
}