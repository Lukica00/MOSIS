package com.luka.mosis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.luka.mosis.databinding.FragmentRegisterBinding
import java.io.File
import java.net.URI


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: StorageReference
    private val user: User by viewModels()
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        storage = FirebaseStorage.getInstance(Firebase.app).reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        binding.toolbar.setupWithNavController(findNavController())
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.registerImage.setImageURI(uri)
                Log.d("PhotoPicker", "Selected URI: $uri")
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
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
        val image = binding.registerImage
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

        image.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        registerButton.setOnClickListener {
            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener(requireActivity()) { task ->
                val taskovi = mutableListOf<Any>()
                if (task.isSuccessful) {
                    if(imageUri!=null){
                        val firebaseImages = storage.child("images/${auth.currentUser?.uid}")
                        firebaseImages.putFile(imageUri!!).continueWithTask { task ->
                            if (task.isSuccessful) {
                                firebaseImages.downloadUrl
                            }else{
                                task.exception?.let {
                                    throw it
                                }
                            }
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                auth.currentUser?.updateProfile(userProfileChangeRequest {
                                    photoUri = task.result
                                    displayName = username.text.toString()
                                })?.addOnCompleteListener {
                                    findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                                }
                            }
                        }
                    }else{
                        auth.currentUser?.updateProfile(userProfileChangeRequest {
                            displayName = username.text.toString()
                        })?.addOnCompleteListener {
                            findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                        }
                    }
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