package com.luka.mosis

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.UnknownServiceException

class MainFragment : Fragment() {
    val user : User by viewModels();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onStart() {
        super.onStart()
        view?.findViewById<Button>(R.id.logout_button)?.setOnClickListener{
            Log.d("GASGAS","GAGAGAGAGA")
            Firebase.auth.signOut()
            user.user = Firebase.auth.currentUser
            findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
        }
    }
}