package com.luka.mosis

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.luka.mosis.databinding.FragmentAddObjectBinding
import com.luka.mosis.databinding.FragmentMainBinding
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

class AddObject : Fragment() {
    private var _binding: FragmentAddObjectBinding? = null
    private val binding get() = _binding!!
    private val user : User by activityViewModels()
    private lateinit var db: FirebaseFirestore
    private var location: MutableLiveData<Location?> = MutableLiveData(null);
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        location.observe(viewLifecycleOwner) {
            binding.dodajObjekat.isEnabled = location.value!=null
        }

        if(ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
                isGranted:Boolean->
            if(isGranted){
                LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                    location.value = it;
                }
            }
        }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                location.value = it;
            }
        }
        binding.dodajObjekat.setOnClickListener {
            val userDb = hashMapOf(
                "location" to GeoPoint(location.value!!.latitude,location.value!!.longitude),
                "type" to 0,
                "name" to binding.addName.text.toString(),
                "latinName" to binding.addLatinName.text.toString(),
                "imageUri" to "",
                "owner" to db.collection("users").document(user.userId.value!!)
            )
            db.collection("posts").add(userDb).addOnSuccessListener {
                findNavController().navigate(R.id.action_addObject_to_mainFragment)
            }

        }
    }
}