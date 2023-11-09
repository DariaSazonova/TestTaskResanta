package com.example.testtaskresanta

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.testtaskresanta.databinding.ActivityMainBinding
import com.example.testtaskresanta.viewModels.MainActivityViewModel
import androidx.activity.viewModels
import com.example.testtaskresanta.models.MarkerData
import com.example.testtaskresanta.services.UsbPortService.Companion.registerMarkerBroadcastReceiver
import com.example.testtaskresanta.services.UsbPortService.Companion.sendParams
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

//    Задача такая
//
//    Написать приложение, на главном экране которого вводятся следующие параметры:
//    долгота, широта, тип метки (точка, техника, человек)
//
//    ниже располагается кнопка отправить, после нажатия на которую данные маппятся в байты и передаются в юсб порт (здесь нужно написать сервис, который может принимать данные с порта и отправлять). Проверить работу юсб ты не сможешь конечно, но главное, как ты напишешь, а не то, как работает
//
//    Будет плюсом:
//    захардкодить массив байт, который ты якобы получил с порта, смаппить его обратно в твою метку и отобразить результирующую долготу, широту и тип на экране

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() =  _binding!!

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerMarkerBroadcastReceiver(viewModel.markerBroadcastReceiver)

        binding.buttonSend.setOnClickListener {
            val longitude = binding.longitude.text.toString().toDoubleOrNull()
            if (longitude != null){
                val latitude = binding.latitude.text.toString().toDoubleOrNull()
                if (latitude != null){
                    sendParams(MarkerData(longitude,latitude, binding.markerTypeSpinner.selectedItem.toString()))
                }
                else {
                    Toast.makeText(this, "Неверные значения Широты", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(this, "Неверные значения Долготы", Toast.LENGTH_SHORT).show()
            }
        }

        // отображение ответа с usb порта

        lifecycleScope.launch {
            viewModel.markerDataResponse
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collect { markerData ->
                    markerData?.let {
                        binding.result.text = "Ответ:\nДолгота: ${markerData.longitude}\nШирота: ${markerData.latitude}\nТип: ${markerData.markerType}"
                    }
                }
        }

        //отображение ошибок, если они были
        lifecycleScope.launch {
            viewModel.errorFromResponse
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collect { error ->
                    error?.let{
                        Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
                    }
                }
        }

    }

    override fun onDestroy() {
        unregisterReceiver(viewModel.markerBroadcastReceiver)
        _binding = null
        super.onDestroy()
    }
}