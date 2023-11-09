package com.example.testtaskresanta

import com.example.testtaskresanta.models.MarkerData
import com.google.gson.Gson
import java.lang.Exception

fun ByteArray.toMarkerData():MarkerData?{
    return try {
        Gson().fromJson(this.toString(Charsets.UTF_8), MarkerData::class.java)
    }
    catch(e:Exception){
        null
    }
}