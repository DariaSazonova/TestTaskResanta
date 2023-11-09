package com.example.testtaskresanta.models

import com.google.gson.Gson

data class MarkerData(
    val longitude: Double,
    val latitude: Double,
    val markerType: String
):DataToBytes {
    override fun toBytes(): ByteArray {
        return this.toString().toByteArray(Charsets.UTF_8)
    }
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

interface DataToBytes{
    fun toBytes(): ByteArray
}