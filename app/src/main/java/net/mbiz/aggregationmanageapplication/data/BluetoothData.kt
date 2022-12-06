package net.mbiz.aggregationmanageapplication.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt

class BluetoothData (
    val bleGatt: BluetoothGatt,
    val bluetoothDeviceStatus : Boolean)