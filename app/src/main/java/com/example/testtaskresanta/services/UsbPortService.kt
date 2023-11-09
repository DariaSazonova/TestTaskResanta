package com.example.testtaskresanta.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.testtaskresanta.models.MarkerData
import java.util.Random


class UsbPortService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        processCommand(intent)
        return START_NOT_STICKY
    }

    private fun processCommand(intent:Intent?){
        // получение данных с интента
        val markerBytes = intent?.getByteArrayExtra(INTENT_MARKER_BYTES)

        //отправка данных по usb порту
        if(markerBytes!=null ) sendDataToUSBPort(markerBytes)
        else{
            sendError( "Не удалось получить входные данные")
        }

        // так, как проверить работает usb или нет не получится,
        // поэтому просто отправляем какой-то ответ
        handleMarkerData(ByteArray(1024))

        startForeground(1, createNotification())
    }

    private fun createNotification(): Notification {

        createNotificationChannel()

        // создание уведомления для сервиса
        val notification = NotificationCompat.Builder(
            applicationContext,
            notificationChannelId
        )
            .setContentTitle("Usb port")
            .build()

        return notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "Notification Channel UsbPort",
                NotificationManager.IMPORTANCE_DEFAULT,
            )

            notificationChannel.enableVibration(true)

            val notificationManager: NotificationManager? =
                ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)

            notificationManager?.createNotificationChannel(
                notificationChannel
            )
        }
    }


    private fun handleMarkerData(byteArray:ByteArray?) {
        if(byteArray!=null) {
            //val response = byteArray.toMarkerData() // полученный массив преобразуем обратно
            // просто что-то возвращаем
            val data = MarkerData(Random().nextDouble(), Random().nextDouble(), "type")
            val intent = Intent(INTENT_MARKER_DATA_UPDATE).apply {
                putExtra(INTENT_MARKER_BYTES, data.toBytes())
            }
            sendBroadcast(intent)
        }
        else{
            sendError("Данные не получены")
        }
    }


    private fun sendDataToUSBPort(data: ByteArray) {
        val usbManager: UsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice: UsbDevice? = usbManager.deviceList.values.firstOrNull {
            it.vendorId == USB_VENDOR_ID && it.productId == USB_PRODUCT_ID
        }

        if (usbDevice != null) {
            val usbInterface: UsbInterface = usbDevice.getInterface(0)
            val usbEndpoint: UsbEndpoint = usbInterface.getEndpoint(0)

            val connection: UsbDeviceConnection = usbManager.openDevice(usbDevice)
            if (connection.claimInterface(usbInterface, true)) {
                val result: Int = connection.bulkTransfer(
                    usbEndpoint,
                    data,
                    data.size,
                    USB_TIMEOUT
                )
                if (result >= 0) {
                    // отправка прошла успешно
                    // принимаем ответ
                    val receivedData = ByteArray(1024) // Размер ответа в байтах
                    val receiveResult: Int = connection.bulkTransfer(
                        usbEndpoint,
                        receivedData,
                        receivedData.size,
                        USB_TIMEOUT
                    )

                    if (receiveResult >= 0) {
                        handleMarkerData(receivedData)
                    } else {
                        sendError("Ошибка при приеме данных")
                    }
                } else {
                    sendError("Ошибка при отправке данных")
                }
                connection.releaseInterface(usbInterface)
            } else {
                sendError("Не удалось получить доступ к интерфейсу USB")
            }
            connection.close()
        } else {
            sendError("Устройство USB не найдено")
        }
    }

    private fun sendError(errorValue: String){
        val intent = Intent(INTENT_MARKER_DATA_UPDATE).apply {
            putExtra(INTENT_SERVICE_ERROR, errorValue)
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val USB_VENDOR_ID = 0x1234 // примерный Vendor ID
        private const val USB_PRODUCT_ID = 0x5678 // примерный Product ID
        private const val USB_TIMEOUT = 0

        private const val INTENT_MARKER_DATA_UPDATE = "markerDataUpdate"
        const val INTENT_MARKER_BYTES = "markerBytes"
        const val INTENT_SERVICE_ERROR = "serviceError"

        private const val notificationChannelId = "NotificationChannelUsbPort"

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        fun Context.registerMarkerBroadcastReceiver(broadcastReceiver: BroadcastReceiver){
            val intentFilter = IntentFilter(INTENT_MARKER_DATA_UPDATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
            }
            else{
                registerReceiver(broadcastReceiver, intentFilter)
            }
        }

        fun Context.sendParams(markerData: MarkerData){
            val intent = Intent(this, UsbPortService::class.java).apply {
                putExtra(UsbPortService.INTENT_MARKER_BYTES, markerData.toBytes())
            }
            startService(intent)
        }
    }
}
