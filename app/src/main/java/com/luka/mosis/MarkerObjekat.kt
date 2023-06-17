package com.luka.mosis

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MarkerObjekat(mapView: MapView, documentFirestore: DocumentSnapshot): Marker(mapView) {
    public var document:DocumentSnapshot? = null
    init {
        document = documentFirestore;
    }
}