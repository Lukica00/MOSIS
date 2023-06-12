package com.luka.mosis

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private val user: User by viewModels();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onStart() {
        super.onStart()
        val registerButton = view?.findViewById<Button>(R.id.register_button)
        registerButton?.isEnabled = false
        val email = view?.findViewById<EditText>(R.id.register_email_edittext)
        var validEmail = !TextUtils.isEmpty(email?.text) && Patterns.EMAIL_ADDRESS.matcher(email?.text).matches();
        val password = view?.findViewById<EditText>(R.id.register_enter_password_edittext)
        val repeatPassword = view?.findViewById<EditText>(R.id.register_repeat_password_edittext)
        var validPassword = false;
        if(!repeatPassword?.text.isNullOrEmpty())
            validPassword = repeatPassword?.text!!.length > 5 && repeatPassword?.text.contentEquals(password?.text);
        val username = view?.findViewById<EditText>(R.id.register_username_edittext)
        val phone = view?.findViewById<EditText>(R.id.register_phone_edittext)
        var validPhone = Patterns.PHONE.matcher(phone?.text).matches();
        email?.doOnTextChanged { text, start, before, count ->
            validEmail = !TextUtils.isEmpty(text) && Patterns.EMAIL_ADDRESS.matcher(text).matches();
            registerButton?.isEnabled = validPassword && validEmail && validPhone && !username?.text.isNullOrEmpty();
        }
        password?.doOnTextChanged { text, start, before, count ->
            if(!text.isNullOrEmpty())
                validPassword = text.length > 5 && text.contentEquals(repeatPassword?.text);
            registerButton?.isEnabled = validPassword && validEmail && validPhone && !username?.text.isNullOrEmpty();
        }
        repeatPassword?.doOnTextChanged { text, start, before, count ->
            if(!text.isNullOrEmpty())
                validPassword = text.length > 5 && text.contentEquals(password?.text);
            registerButton?.isEnabled = validPassword && validEmail && validPhone && !username?.text.isNullOrEmpty();
        }
        phone?.doOnTextChanged { text, start, before, count ->
            validPhone = Patterns.PHONE.matcher(text).matches();
            registerButton?.isEnabled = validPassword && validEmail && validPhone && !username?.text.isNullOrEmpty();
        }
        registerButton?.isEnabled = validPassword && validEmail && validPhone && !username?.text.isNullOrEmpty();
        registerButton?.setOnClickListener {
            auth.createUserWithEmailAndPassword(email?.text.toString(), password?.text.toString())
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this.context, "Decko ti si registrovan upravo", Toast.LENGTH_LONG).show();
                    user.user = auth.currentUser
                    findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                } else {
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this.context, "Dalje neces moci", Toast.LENGTH_LONG).show();
                }
            } }
    }
}