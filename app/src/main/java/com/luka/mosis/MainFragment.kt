package com.luka.mosis

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import coil.load
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.luka.mosis.databinding.FragmentMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val user : User by activityViewModels()
    private lateinit var db: FirebaseFirestore
    lateinit var mapa : MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        db = Firebase.firestore
        MenuCompat.setGroupDividerEnabled(binding.navView.menu,true)
        binding.navView.setCheckedItem(R.id.explore)
        binding.toolbar.setupWithNavController(findNavController(), AppBarConfiguration(setOf(R.id.mainFragment),binding.drawerLayout))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        user.email.observe(viewLifecycleOwner){
            Log.d("LOGOG eMail", it.toString())
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.menu_header_bottom).text =
                it
        }
        user.displayName.observe(viewLifecycleOwner){
            Log.d("LOGOG dNAme", it.toString())
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.menu_header_top).text = it
        }
        user.photoUri.observe(viewLifecycleOwner){
            Log.d("LOGOG Uri", it.toString())
            binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.menu_header_image)
                .load(it)
        }
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.logout_item -> {
                    Firebase.auth.signOut()
                    user.setFirebaseUser(Firebase.auth.currentUser)
                    findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
                }
                else -> {
                }
            }
            true
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.filter -> {
                    binding.drawerLayout.openDrawer(binding.navView2)
                }
                else -> {
                }
            }
            true
        }

        binding.mainFab.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_addObject)
        }
        db.collection("posts").get().addOnSuccessListener {
            for (document in it){
                if(document.data["location"]!=null){
                    val marker = Marker(mapa)
                    val loc = document.data["location"] as com.google.firebase.firestore.GeoPoint
                    marker.position = GeoPoint(loc.latitude, loc.longitude)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    mapa.overlays.add(marker)
                }
            }
        }
        val ctx: Context? = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx!!))
        mapa = binding.mapa
        mapa.setMultiTouchControls(true)
        if(ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                    isGranted:Boolean->
                if(isGranted){
                    setMyLocationOverlay()
                }
            }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            setMyLocationOverlay()
        }
        mapa.controller.setZoom(15.0)
        mapa.controller.setCenter(GeoPoint(43.3209, 21.8958))
        //mapa.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val rotacijaOverlay = RotationGestureOverlay(mapa)
        rotacijaOverlay.isEnabled = true
        mapa.overlays.add(rotacijaOverlay)
        val compassOverlay = object: CompassOverlay(context, mapa){
            override fun draw(c: Canvas?, pProjection: Projection?) {
                drawCompass(c, -mapa.mapOrientation, pProjection?.screenRect)
            }

            override fun onSingleTapUp(e: MotionEvent?, mapView: MapView?): Boolean {
                mapa.setMapOrientation(0.0f,false);
                return true
            }
        }
        compassOverlay.enableCompass()
        mapa.overlays.add(compassOverlay)
    }
    private fun setMyLocationOverlay(){
        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireActivity()),mapa)
        myLocationOverlay.enableMyLocation()
        mapa.overlays.add(myLocationOverlay)
    }
    override fun onStart() {
        super.onStart()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        mapa.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapa.onPause()
    }
}