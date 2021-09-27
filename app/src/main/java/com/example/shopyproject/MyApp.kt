package com.example.shopyproject

import android.app.Application
import com.example.shopyproject.fcm.VolleyHelper

class MyApp : Application(){

    companion object{
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate() {
        super.onCreate()
        volleyHelper = VolleyHelper.getInstance(this)
    }
}