package com.luka.mosis

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.luka.mosis.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val user: User by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onStart() {
        super.onStart()

        user.setFirebaseUser(auth.currentUser)
        if (user.userId.value != null) {
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }
        val loginButton = binding.loginButton
        loginButton.isEnabled = false
        val signupButton = binding.signupButton
        val email = binding.loginEmailEdittext
        var validEmail = !TextUtils.isEmpty(email.text) && Patterns.EMAIL_ADDRESS.matcher(email.text).matches()
        val password = binding.loginPasswordEdittext
        var validPassword = false
        if(!password.text.isNullOrEmpty())
            validPassword = password.text!!.length > 5
        email.doOnTextChanged { text, _, _, _ ->
            validEmail = !TextUtils.isEmpty(text) && Patterns.EMAIL_ADDRESS.matcher(text!!).matches()
            loginButton.isEnabled = validPassword && validEmail
        }
        password.doOnTextChanged { text, _, _, _ ->
            if(!text.isNullOrEmpty())
                validPassword = text.length > 5
            loginButton.isEnabled = validPassword && validEmail
        }
        loginButton.isEnabled = validPassword && validEmail
        loginButton.setOnClickListener {
            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        user.setFirebaseUser(auth.currentUser)
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        Log.w("TAG", "signInWithEmail:failure", task.exception)
                        Toast.makeText(this.context, "Vudju lav or vudju lajk", Toast.LENGTH_LONG).show()
                    }
                }
        }
        signupButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}