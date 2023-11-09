package com.example.testtaskresanta.viewModels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.testtaskresanta.models.MarkerData
import com.example.testtaskresanta.services.UsbPortService
import com.example.testtaskresanta.toMarkerData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class MainActivityViewModel: ViewModel() {
    private val _markerDataResponse = MutableStateFlow<MarkerData?>(null)
    val markerDataResponse: StateFlow<MarkerData?> = _markerDataResponse

    private val _errorFromResponse = MutableStateFlow<String?>(null)
    val errorFromResponse: StateFlow<String?> = _errorFromResponse


     val markerBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getByteArrayExtra(UsbPortService.INTENT_MARKER_BYTES)?.let {
                if (it.isNotEmpty()){
                    _markerDataResponse.value = it.toMarkerData()
                }
            }
            intent.getStringExtra(UsbPortService.INTENT_SERVICE_ERROR)?.let{
                _errorFromResponse.value = it
            }
        }
    }
}