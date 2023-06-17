package com.luka.mosis

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.DateFormat
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.luka.mosis.databinding.FragmentAddObjectBinding
import java.io.File
import java.util.Calendar
import java.util.Date

class AddObject : Fragment() {
    private var _binding: FragmentAddObjectBinding? = null
    private val binding get() = _binding!!
    private val user : User by activityViewModels()
    private lateinit var db: FirebaseFirestore
    private var location: MutableLiveData<Location?> = MutableLiveData(null)
    private lateinit var pickMedia: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private var imageUriTemp: Uri? = null
    private lateinit var storage: StorageReference
    private var locEnabled = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddObjectBinding.inflate(inflater,container,false)
        binding.toolbar.setupWithNavController(findNavController())
        setButtonEnabled()
        storage = FirebaseStorage.getInstance(Firebase.app).reference

        val tezine = arrayOf("Vrlo lako", "Lako", "Osrednje", "Teže", "Baš teško")
        val adapter1 = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item,tezine)
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.addTezinaSpinner.adapter = adapter1

        val tipovi = arrayOf("Biljka", "Životinja", "Gljiva")
        val adapter2 = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item,tipovi)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.addTipSpinner.adapter = adapter2

        pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    if(result.data!=null && result.data!!.data !=null){
                        imageUri = result.data!!.data as Uri
                        binding.addImage.setImageURI(imageUri)
                    }else{
                        imageUri = imageUriTemp
                        binding.addImage.setImageURI(imageUriTemp)
                    }
                    Log.d("PhotoPicker", "Selected URI: $imageUri")
                    setButtonEnabled()
                }
                else -> {
                    Log.d("PhotoPicker", "Otkazano")
                }
            }
        }

        return binding.root
    }

    fun setButtonEnabled(){
        binding.dodajObjekat.isEnabled = locEnabled && imageUri!=null && !binding.addName.text.isNullOrEmpty()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        location.observe(viewLifecycleOwner) {
            locEnabled = location.value!=null
            setButtonEnabled()
        }
        binding.addName.doOnTextChanged { text, _, _, _ ->  setButtonEnabled()}
        if(ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
                isGranted:Boolean->
            if(isGranted){
                LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                    location.value = it
                }
            }
        }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                location.value = it
            }
        }

        binding.addImage.setOnClickListener {
            val galleryintent = Intent(Intent.ACTION_GET_CONTENT, null)
            galleryintent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val path = File(requireActivity().filesDir, "Pictures")
            if (!path.exists()) path.mkdirs()
            val imagege = File(path, "Slika_${
                DateFormat.getDateTimeInstance().format(Date())
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

        binding.dodajObjekat.setOnClickListener {
            val postId = db.collection("posts").document().id
            val firebaseImages = storage.child("images/plants/${postId}")
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
                    db.collection("posts").document(postId).update("imageUri", task.result)
                }
            }
            val userDb = hashMapOf(
                "location" to GeoPoint(location.value!!.latitude,location.value!!.longitude),
                "type" to 0,
                "name" to binding.addName.text.toString(),
                "latinName" to binding.addLatinName.text.toString(),
                "desc" to binding.addOpis.text.toString(),
                "diff" to binding.addTezina.text.toString(),
                "diffNum" to binding.addTezinaSpinner.selectedItemPosition,
                "type" to binding.addTipSpinner.selectedItemPosition,
                "imageUri" to "",
                "owner" to db.collection("users").document(user.userId.value!!),
                "datum" to Calendar.getInstance().time
            )
            db.collection("posts").document(postId).set(userDb).addOnSuccessListener {
                findNavController().navigate(R.id.action_addObject_to_mainFragment)
            }

        }
    }
}