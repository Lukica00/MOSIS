package com.luka.mosis.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.luka.mosis.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this.context, "ULOGOVAN SI BAJO", Toast.LENGTH_LONG).show();
        }
        val login_button = view?.findViewById<Button>(R.id.login_button)
        val signup_button = view?.findViewById<Button>(R.id.signup_button)
        val email = view?.findViewById<EditText>(R.id.email_edittext)
        val password = view?.findViewById<EditText>(R.id.password_edittext)
        login_button?.setOnClickListener {
            auth.signInWithEmailAndPassword(email?.text.toString(), password?.text.toString())
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this.context, "Decko ti si loginovan upravo", Toast.LENGTH_LONG).show();
                        val user = auth.currentUser
                    } else {
                        Log.w("TAG", "signInWithEmail:failure", task.exception)
                        Toast.makeText(this.context, "Vudju lav or vudju lajk", Toast.LENGTH_LONG).show();
                    }
                }
        }
        signup_button?.setOnClickListener {
            auth.createUserWithEmailAndPassword(email?.text.toString(), password?.text.toString())
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this.context, "Decko ti si registrovan upravo", Toast.LENGTH_LONG).show();
                        val user = auth.currentUser
                    } else {
                        Log.w("TAG", "signInWithEmail:failure", task.exception)
                        Toast.makeText(this.context, "Dalje neces moci", Toast.LENGTH_LONG).show();
                    }
                }
        }
    }
}