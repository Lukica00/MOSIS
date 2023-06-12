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

class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var auth: FirebaseAuth
    private val user: User by viewModels();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    public override fun onStart() {
        super.onStart()

        user.user = auth.currentUser
        if (user.user != null) {
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }
        val login_button = view?.findViewById<Button>(R.id.login_button)
        login_button?.isEnabled = false;
        val signup_button = view?.findViewById<Button>(R.id.signup_button)
        val email = view?.findViewById<EditText>(R.id.login_email_edittext)
        var validEmail = !TextUtils.isEmpty(email?.text) && Patterns.EMAIL_ADDRESS.matcher(email?.text).matches();
        val password = view?.findViewById<EditText>(R.id.login_password_edittext)
        var validPassword = false;
        if(!password?.text.isNullOrEmpty())
            validPassword = password?.text!!.length > 5;
        email?.doOnTextChanged { text, start, before, count ->
            validEmail = !TextUtils.isEmpty(text) && Patterns.EMAIL_ADDRESS.matcher(text).matches();
            login_button?.isEnabled = validPassword && validEmail;
        }
        password?.doOnTextChanged { text, start, before, count ->
            if(!text.isNullOrEmpty())
                validPassword = text.length > 5;
            login_button?.isEnabled = validPassword && validEmail;
        }
        login_button?.isEnabled = validPassword && validEmail;
        login_button?.setOnClickListener {
            auth.signInWithEmailAndPassword(email?.text.toString(), password?.text.toString())
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                        val user = auth.currentUser
                    } else {
                        Log.w("TAG", "signInWithEmail:failure", task.exception)
                        Toast.makeText(this.context, "Vudju lav or vudju lajk", Toast.LENGTH_LONG).show();
                    }
                }
        }
        signup_button?.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}