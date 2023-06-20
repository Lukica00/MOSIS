package com.luka.mosis

import android.app.Activity
import android.content.Intent
import android.icu.text.DateFormat.getDateTimeInstance
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.luka.mosis.databinding.FragmentRegisterBinding
import java.io.File
import java.util.Date


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var storage: StorageReference
    private val user: User by activityViewModels()
    private lateinit var pickMedia: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private var imageUriTemp: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        storage = FirebaseStorage.getInstance(Firebase.app).reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        binding.toolbar.setupWithNavController(findNavController())
        pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    if(result.data!=null && result.data!!.data !=null){
                        imageUri = result.data!!.data as Uri
                        binding.registerImage.setImageURI(imageUri)
                    }else{
                        imageUri = imageUriTemp
                        binding.registerImage.setImageURI(imageUriTemp)
                    }
                    Log.d("PhotoPicker", "Selected URI: $imageUri")
                }
                else -> {
                    Log.d("PhotoPicker", "Otkazano")
                }
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
            validPassword = repeatPassword.text!!.length > 5 && repeatPassword.text!!.contentEquals(password.text)
        val username = binding.registerUsernameEdittext
        val phone = binding.registerPhoneEdittext
        var validPhone = Patterns.PHONE.matcher(phone.text.toString()).matches()
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
            val galleryintent = Intent(Intent.ACTION_GET_CONTENT, null)
            galleryintent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val path = File(requireActivity().filesDir, "Pictures")
            if (!path.exists()) path.mkdirs()
            val imagege = File(path, "Slika_${
                getDateTimeInstance().format(Date())
            }.jpg")

            imageUriTemp = FileProvider.getUriForFile(requireActivity(), "com.luka.mosis.fileprovider", imagege)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriTemp)

            val chooser = Intent(Intent.ACTION_CHOOSER)
            chooser.putExtra(Intent.EXTRA_TITLE, "Select from:")
            chooser.putExtra(Intent.EXTRA_INTENT, galleryintent)
            val intentArray = arrayOf(cameraIntent)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            pickMedia.launch(chooser)
        }
        registerButton.setOnClickListener {
            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    user.setFirebaseUser(auth.currentUser)
                    user.displayName.value = username.text.toString()
                    if(imageUri!=null){
                        val firebaseImages = storage.child("images/profiles/${auth.currentUser?.uid}")
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
                                    user.setFirebaseUser(auth.currentUser)
                                }
                                user.photoUri.value = task.result
                                db.collection("users").document(user.userId.value!!).update("photoUri", task.result)
                            }
                        }
                    }else{
                        auth.currentUser?.updateProfile(userProfileChangeRequest {
                            displayName = username.text.toString()
                        })?.addOnCompleteListener {
                            user.setFirebaseUser(auth.currentUser)
                        }
                    }
                    val posts : ArrayList<String> = ArrayList()
                    val userDb = hashMapOf(
                        "userId" to user.userId.value,
                        "score" to 0,
                        "posts" to posts,
                        "displayName" to user.displayName.value,
                        "photoUri" to ""
                    )
                    db.collection("users").document(user.userId.value!!).set(userDb)
                    .addOnSuccessListener { documentReference ->
                        Log.d("Doc", "DocumentSnapshot added with ID: ${documentReference}")
                    }
                    .addOnFailureListener { e ->
                        Log.w("Doc", "Error adding document", e)
                    }
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