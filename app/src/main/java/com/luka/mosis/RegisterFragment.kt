package com.luka.mosis

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.luka.mosis.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val user: User by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        binding.toolbar.setupWithNavController(findNavController())
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val registerButton = binding.registerButton
        registerButton.isEnabled = false
        val email = binding.registerEmailEdittext
        var validEmail = !TextUtils.isEmpty(email.text) && Patterns.EMAIL_ADDRESS.matcher(email.text).matches()
        val password = binding.registerEnterPasswordEdittext
        val repeatPassword = binding.registerRepeatPasswordEdittext
        var validPassword = false
        if(!repeatPassword.text.isNullOrEmpty())
            validPassword = repeatPassword.text.length > 5 && repeatPassword.text.contentEquals(password.text)
        val username = binding.registerUsernameEdittext
        val phone = binding.registerPhoneEdittext
        var validPhone = Patterns.PHONE.matcher(phone.text).matches()
        email.doOnTextChanged { text, _, _, _ ->
            validEmail = !TextUtils.isEmpty(text) && Patterns.EMAIL_ADDRESS.matcher(text!!).matches()
            registerButton.isEnabled = validPassword && validEmail && validPhone && !username.text.isNullOrEmpty()
        }
        password.doOnTextChanged { text, _, _, _ ->
            if(!text.isNullOrEmpty())
                validPassword = text.length > 5 && text.contentEquals(repeatPassword.text)
            registerButton.isEnabled = validPassword && validEmail && validPhone && !username.text.isNullOrEmpty()
        }
        repeatPassword.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty())
                validPassword = text.length > 5 && text.contentEquals(password.text)
            registerButton.isEnabled =
                validPassword && validEmail && validPhone && !username.text.isNullOrEmpty()
        }
        phone.doOnTextChanged { text, _, _, _ ->
            validPhone = !TextUtils.isEmpty(text) && Patterns.PHONE.matcher(text!!).matches()
            registerButton.isEnabled = validPassword && validEmail && validPhone && !username.text.isNullOrEmpty()
        }
        registerButton.isEnabled = validPassword && validEmail && validPhone && !username.text.isNullOrEmpty()
        registerButton.setOnClickListener {
            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this.context, "Decko ti si registrovan upravo", Toast.LENGTH_LONG).show()
                    user.user = auth.currentUser
                    findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                } else {
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this.context, "Dalje neces moci", Toast.LENGTH_LONG).show()
                }
            } }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}