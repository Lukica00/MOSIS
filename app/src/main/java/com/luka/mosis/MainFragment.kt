package com.luka.mosis

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import coil.load
import com.google.android.gms.location.LocationServices
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.luka.mosis.databinding.FragmentMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Date
import java.util.Locale


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val user : User by activityViewModels()
    private var location: Location? =null
    private lateinit var db: FirebaseFirestore
    lateinit var mapa : MapView
    private var markeri : List<MarkerObjekat?>? = null
    private var picker: MaterialDatePicker<androidx.core.util.Pair<Long,Long>>? = null
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
        mapa = binding.mapa
        binding.navView.setCheckedItem(R.id.explore)
        binding.toolbar.setupWithNavController(findNavController(), AppBarConfiguration(setOf(R.id.mainFragment),binding.drawerLayout))
        binding.filterTezina.setValues(0.0f,4.0f)
        binding.filterToggle.check(R.id.filter_biljka)
        binding.filterToggle.check(R.id.filter_zivotinja)
        binding.filterToggle.check(R.id.filter_gljiva)
        return binding.root
    }
    fun slusacFiltera(){
        closeAllInfoWindowsOn(mapa)
        if(markeri!=null){
            val startDate = Date((picker!!.selection as androidx.core.util.Pair<Long,Long>).first)
            val endDate = Date((picker!!.selection as androidx.core.util.Pair<Long,Long>).second)
            val biljka = binding.filterBiljka.isChecked
            val zivotinja = binding.filterZivotinja.isChecked
            val gljiva = binding.filterGljiva.isChecked
            val imeBox = binding.filterIme.text
            val tezRange = binding.filterTezina.values
            val moje = binding.filterMoje.isChecked
            val omiljeno = binding.filterOmiljeno.isChecked
            val radiusRange = binding.filterRadius.text.toString().toFloatOrNull()
            for (marker in markeri!!){
                if(marker!=null){
                    val vreme = (marker.document!!.data!!["datum"] as Timestamp).toDate()
                    val posle = vreme.after(startDate)
                    val pre = vreme.before(endDate)
                    val vremeOk = posle&&pre

                    val tip = marker.document!!.data!!["type"] as Long
                    val tipOk = biljka && tip==0L || zivotinja && tip==1L || gljiva && tip==2L

                    var imeOk = true
                    val ime = marker.document!!.data!!["name"] as String
                    val latinIme = marker.document!!.data!!["latinName"] as String
                    if(!imeBox.isNullOrEmpty()){
                        imeOk = ime.contains(imeBox.toString()) || latinIme.contains(imeBox.toString())
                    }

                    val tez = (marker.document!!.data!!["type"] as Long).toFloat()
                    val tezOk = tezRange[0] <= tez && tez <= tezRange[1]

                    var isMoj = true
                    if(moje){
                        isMoj = (marker.document!!.data!!["owner"] as DocumentReference).id.contains(user.userId.value!!)
                    }
                    var isOmiljen = true
                    if(omiljeno){
                        isOmiljen = user.posts.value!!.contains(marker.document!!.id)
                    }

                    var razdaljinaOk = true
                    if (location!=null && radiusRange!=null){
                        val dist = FloatArray(1)
                        Location.distanceBetween(
                            location!!.latitude,
                            location!!.longitude,
                            marker.position.latitude,
                            marker.position.longitude,
                            dist
                        )
                        razdaljinaOk = dist[0]/1000<=radiusRange
                    }
                    marker.isEnabled = vremeOk && tipOk && imeOk && tezOk && isMoj && razdaljinaOk && isOmiljen
                }
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        picker = MaterialDatePicker.Builder.dateRangePicker().setSelection(
            androidx.core.util.Pair(
                MaterialDatePicker.thisMonthInUtcMilliseconds(),
                MaterialDatePicker.todayInUtcMilliseconds()+86400000L
            )
        ).build()

        picker!!.addOnPositiveButtonClickListener{
            slusacFiltera()
        }
        binding.filterToggle.addOnButtonCheckedListener{ _, _, _ -> slusacFiltera() }
        binding.filterIme.doOnTextChanged { _, _, _, _ ->  slusacFiltera()}
        binding.filterTezina.addOnChangeListener { _, _, _ ->  slusacFiltera()}
        binding.filterRadius.doOnTextChanged { _, _, _, _->  slusacFiltera()}
        binding.filterMoje.setOnCheckedChangeListener { _, _ -> slusacFiltera() }
        binding.filterOmiljeno.setOnCheckedChangeListener { _, _ -> slusacFiltera() }


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
        binding.filterDatum.setOnClickListener {
            picker!!.show(requireActivity().supportFragmentManager,"tag")
        }
        db.collection("posts").get().addOnSuccessListener {
            markeri = List<MarkerObjekat?>(it.size()){i->
                    val document = it.documents[i]
                    if (document == null || document.data!!["location"] == null)null
                    else{
                        val marker = MarkerObjekat(mapa, document)
                        val loc = document.data!!["location"] as com.google.firebase.firestore.GeoPoint
                        marker.position = GeoPoint(loc.latitude, loc.longitude)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = document.data!!["name"] as String
                        marker.snippet = document.data!!["latinName"] as String
                        val opis = document.data!!["desc"] as String
                        if(opis != "")
                            marker.subDescription = getString(R.string.bubble_opis, document.data!!["desc"] as String)
                        marker.infoWindow = object: MarkerInfoWindow(R.layout.marker_bubble,mapa){
                            var prvi = true
                            var fav: Boolean?
                            init {
                                fav = user.posts.value?.contains(document.reference.id)
                                Log.d("TAGG", user.posts.value?.size.toString())
                                (document.data!!["owner"] as DocumentReference).get().addOnSuccessListener {
                                    marker?.infoWindow?.view?.findViewById<TextView>(R.id.bubble_author)?.text = getString(R.string.bubble_author,it.data!!["displayName"] as String)
                                }
                                val button = mView.findViewById<Button>(R.id.bubble_moreinfo)
                                if(fav == true){
                                    button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_favorite_24, 0, 0, 0)
                                }else{
                                    button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_favorite_border_24, 0, 0, 0)
                                }
                                button.setOnClickListener {
                                    fav = !fav!!
                                    val ref = document.data!!["owner"] as DocumentReference
                                    if(fav==true){
                                        user.posts.value!!.add(document.id)
                                        db.collection("users").document(ref.id).get().addOnSuccessListener {
                                            db.collection("users").document(ref.id).update("score", (it.data!!["score"] as Long) + 1L)
                                        }
                                        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_favorite_24, 0, 0, 0)
                                    }else{
                                        user.posts.value!!.remove(document.id)
                                        db.collection("users").document(ref.id).get().addOnSuccessListener {
                                            db.collection("users").document(ref.id).update("score", (it.data!!["score"] as Long) - 1L)
                                        }
                                        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_favorite_border_24, 0, 0, 0)
                                    }
                                    db.collection("users").document(user.userId.value!!).update("posts",user.posts.value!!)
                                }
                            }
                            override fun onOpen(item: Any?) {
                                closeAllInfoWindowsOn(mapa)
                                super.onOpen(item)
                                if(prvi){
                                    prvi = false
                                    if(document.data!!["imageUri"] as String == ""){
                                        db.collection("posts").document(document.reference.id).get().addOnSuccessListener {
                                            document.data!!["imageUri"] = it.data!!["imageUri"]
                                            marker?.infoWindow?.view?.findViewById<ImageView>(R.id.bubble_image_moj)?.load(Uri.parse(it.data!!["imageUri"] as String))
                                        }
                                    }
                                    else
                                        marker?.infoWindow?.view?.findViewById<ImageView>(R.id.bubble_image_moj)?.load(Uri.parse(document.data!!["imageUri"] as String))
                                }
                            }
                        }
                        val tezinaNum = resources.getStringArray(R.array.tezina_spinner)[(document.data!!["diffNum"] as Long).toInt()]
                        val tezina = document.data!!["diff"] as String
                        if(tezina == "") marker.infoWindow.view.findViewById<TextView>(R.id.bubble_tezina).text = getString(R.string.bubble_dolazak_kratak, tezinaNum)
                            else marker.infoWindow.view.findViewById<TextView>(R.id.bubble_tezina).text = getString(R.string.bubble_dolazak, tezinaNum, document.data!!["diff"] as String)
                        marker.infoWindow.view.findViewById<TextView>(R.id.bubble_datum).text = getString(R.string.bubble_datum, SimpleDateFormat("dd. MM. yyyy.", Locale.getDefault()).format((document.data!!["datum"] as Timestamp).toDate()))
                        when(document.data!!["type"] as Long){
                            0L ->{
                                marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.outline_yard_24, null)
                            }
                            1L ->{
                                marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.outline_pets_24, null)
                            }
                            2L ->{
                                marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.mushroom_outline, null)
                            }
                            else -> {
                            }
                        }
                        mapa.overlays.add(marker)
                        marker
                    }
                }
            slusacFiltera()
        }
        val ctx: Context? = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx!!))
        mapa.setMultiTouchControls(true)
        if(ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                    isGranted:Boolean->
                if(isGranted){
                    setMyLocationOverlay()
                    LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                        location = it
                    }
                }
            }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else{
            setMyLocationOverlay()
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                location = it
            }
        }
        mapa.controller.setZoom(15.0)
        mapa.controller.setCenter(GeoPoint(43.3209, 21.8958))

        val rotacijaOverlay = RotationGestureOverlay(mapa)
        rotacijaOverlay.isEnabled = true
        mapa.overlays.add(rotacijaOverlay)
        val compassOverlay = object: CompassOverlay(context, mapa){
            override fun draw(c: Canvas?, pProjection: Projection?) {
                drawCompass(c, -mapa.mapOrientation, pProjection?.screenRect)
            }

            override fun onSingleTapUp(e: MotionEvent?, mapView: MapView?): Boolean {
                mapa.setMapOrientation(0.0f,false)
                return true
            }
        }
        val scaleBar = ScaleBarOverlay(mapa)
        mapa.overlays.add(scaleBar)
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