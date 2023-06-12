package com.luka.mosis

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser

class User: ViewModel(){
    var user: FirebaseUser? = null
    set(value) {
        field = value
    }
    get() = field

}
