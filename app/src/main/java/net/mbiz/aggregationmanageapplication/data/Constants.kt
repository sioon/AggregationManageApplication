package net.mbiz.aggregationmanageapplication.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.IntentFilter
import net.mbiz.aggregationmanageapplication.adapter.BluetoothRecyclerAdapter
import net.mbiz.aggregationmanageapplication.adapter.MyBluetoothRecyclerAdapter
import net.mbiz.aggregationmanageapplication.receiver.BroadcastReceiver
import java.io.InputStream
import java.io.OutputStream

class Constants (){
    companion object{

        // used to identify adding bluetooth names
        const val REQUEST_ENABLE_BT = 1
        // used to request fine location permission
        const val REQUEST_ALL_PERMISSION = 2
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        var mDevices: Set<BluetoothDevice>? = null
        var mPairedDeviceCount : Int = 0
        var mSocket: BluetoothSocket? = null
        var mInputStream:  InputStream? = null
        var mRemoteDevice: BluetoothDevice? = null
        var mWorkerThread: Thread? = null
        var readBufferPositon = 0 //버퍼 내 수신 문자 저장 위치

        lateinit var readBuffer: ByteArray //수신 버퍼

        var mDelimiter: Byte = 10
        var devicesArr = java.util.ArrayList<BluetoothDevice>()
        var barcodeList : ArrayList<BarcodeData> = arrayListOf<BarcodeData>()
        var myReceiver : BroadcastReceiver? = null
        var stateFilter : IntentFilter? = null

        var bluetoothAdapter : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        var bluetoothDeviceCheck: String? = null
        var scanCount = 0
        var overlapCheck : Boolean = true
        var bluetoothDeviceArr : ArrayList<BluetoothDevice> = ArrayList()
        var listItems = java.util.ArrayList<BluetoothDevice>()
        var mOutputStream: OutputStream? = null

        lateinit var scanRecyclerViewAdapter : BluetoothRecyclerAdapter
        lateinit var registrationclerViewAdapter : MyBluetoothRecyclerAdapter

        //사용자 BLE UUID Service/Rx/Tx
        const val SERVICE_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
        const val CHARACTERISTIC_COMMAND_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
        const val CHARACTERISTIC_RESPONSE_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
        val UUID_DATA_WRITE = "38eb4a84-c570-11e3-9507-0002a5d5c51b"

        //BluetoothGattDescriptor 고정
        const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    }
}