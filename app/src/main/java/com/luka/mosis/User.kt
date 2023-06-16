package com.luka.mosis

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
class User: ViewModel(){
    var displayName = MutableLiveData<String?>()
    var photoUri =  MutableLiveData<Uri?>()
    var posts =   MutableLiveData<List<String?>>()
    var score =  MutableLiveData<Number?>()
    var userId =  MutableLiveData<String?>()
    var email =  MutableLiveData<String?>()
    fun setFirebaseUser(user:FirebaseUser?){
        if(user==null){
            displayName.value = null
            photoUri.value = null
            userId.value = null
            email.value = null
        }else{
            displayName.value = user.displayName
            photoUri.value = user.photoUrl
            userId.value = user.uid
            email.value = user.email
        }
    }
}
