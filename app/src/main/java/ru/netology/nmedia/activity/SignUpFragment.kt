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
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.SignUpViewModel

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by viewModels()
    /*
    private val viewModel: SignUpViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
    )
    */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(
            inflater,
            container,
            false
        )

        binding.bSignUp.setOnClickListener {
            val userName = binding.name.text.toString().trim();
            val userLogin = binding.login.text.toString().trim();
            val password = binding.pass.text.toString().trim();
            val password2 = binding.pass2.text.toString().trim();

            if (password == password2 && password != "" && userLogin != "" && userName != "") {
                viewModel.registerUser(userLogin, password, userName)
            }
        }

        binding.buttonSignIn.setOnClickListener {
            findNavController().navigate(
                R.id.action_signUpFragment_to_signInFragment
            )
        }

        viewModel.data.observe(viewLifecycleOwner) { state ->
            if (viewModel.isAuthorized) {
                binding.textMessage.text = ""
                findNavController().navigateUp() //закрыть текущий фрагмент и вернуться к предыдущему
            } else {
                binding.textMessage.text =
                    "Fill in all the fields correctly."
            }
        }

        return binding.root
    }
}