package com.luka.mosis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser

class User: ViewModel(){
    val user =  MutableLiveData<FirebaseUser?>()
}
