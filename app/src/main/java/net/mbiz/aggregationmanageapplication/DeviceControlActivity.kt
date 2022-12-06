package net.mbiz.aggregationmanageapplication

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.os.HandlerCompat
import net.mbiz.aggregationmanageapplication.util.BluetoothUtils
import java.util.*


private val TAG = "gattClienCallback"

class DeviceControlActivity(private val context: Context?, private var bluetoothGatt: BluetoothGatt?) {

    private var device : BluetoothDevice? = null

    var statusTxt: String = ""
    var txtRead: String = ""

    var isStatusChange: Boolean = false
    var isTxtRead: Boolean = false

    private val mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    val stateFilter = IntentFilter()

    private val gattCallback : BluetoothGattCallback = object : BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if( status == BluetoothGatt.GATT_FAILURE ) {
                disconnectGattServer()
                return
            } else if( status != BluetoothGatt.GATT_SUCCESS ) {
                disconnectGattServer()
                return
            }
            if( newState == BluetoothProfile.STATE_CONNECTED ) {
                // update the connection status message

                statusTxt = "Connected"
                Log.d(TAG, "Connected to the GATT server")
//                if (context?.let {
//                        ActivityCompat.checkSelfPermission(
//                            it,
//                            Manifest.permission.BLUETOOTH_CONNECT
//                        )
//                    } != PackageManager.PERMISSION_GRANTED
//                ) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return
//                }
//                Log.d(TAG, "여기 까진 옴??")
                // 얘는 체크하지마라 치훈아 이거 permisson check하면 값안넘어간다@@@@
                gatt?.discoverServices()
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                disconnectGattServer()
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status !== BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery failed, status: $status")
                return
            }

//            // check if the discovery failed
//            if (status != BluetoothGatt.GATT_SUCCESS) {
//                Log.e(TAG, "Device service discovery failed, status: $status")
//                val ch = gatt!!.getService(Application.SEVICE)
//                    .getCharacteristic(SampleApplication.CHARACTERISTIC_NOTY)
//            }

            val device = gatt?.device
            val address = device?.address
            var str_data = "\u0010CT~~CD,~CC^~CT~\n" +
                    "^XA~TA000~JSN^LT0^MNW^MTD^PON^PMN^LH0,0^JMA^PR6,6~SD26^JUS^LRN^CI0^XZ\n" +
                    "^XA\n" +
                    "^MMT\n" +
                    "^PW831\n" +
                    "^LL0406\n" +
                    "^LS0\n" +
                    "^BY1,1,150^FT240,250^BCN,,N,N\n" +
                    "^FD>:00#barcode#^FS\n" +
                    "^FT100,320^A0N,40,50^FH\\^FD(00)#barcode#^FS\n" +
                    "^FT100,70^A0N,40,50^FH\\^FDSSCC^FS\n" +
                    "^PQ1,0,1,Y^XZ"

            val charSet = Charsets.UTF_8 //캐릭터셋 선언
            var byt_arr = str_data.toByteArray(charSet) //문자열을 바이트로 변환
            var data_uuid = UUID.nameUUIDFromBytes(byt_arr) //바이트값을 uuid로 생성
            var uuid : UUID = UUID.randomUUID()

            Log.e(TAG, "data_uuid: ${data_uuid}")
            val writeCharacteristic : BluetoothGattCharacteristic = BluetoothGattCharacteristic(uuid,BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE)
            writeCharacteristic.setValue(byt_arr)
//            gatt?.writeCharacteristic(writeCharacteristic)

            if (status === BluetoothGatt.GATT_SUCCESS) {
                val services = gatt!!.services
                for (service in services) {
                    val characteristics = service.characteristics
                    Log.e(TAG, "===================================================================================")
                    Log.e(TAG, "service.uuid : ${service.uuid}")
                    for (characteristic in characteristics) {
                        ///Once you have a characteristic object, you can perform read/write
                        //operations with it
                        Log.e(TAG, "characteristic.uuid : ${characteristic.uuid}")
                        if(characteristic.properties == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT){
                            Log.e(TAG, "characteristic.WRITE_TYPE_DEFAULT : ${characteristic.uuid}")

                        }
                        characteristic.setValue(byt_arr);
                        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        gatt.writeCharacteristic(characteristic);
                    }
                    Log.e(TAG, "===================================================================================")
                }
            }

            // log for successful discovery
            Log.d(TAG, "Services discovery is successful")

        }


//        private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic,data:ByteArray){
//           characteristic.setValue(data)
//            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
//            bluetoothGatt?.writeCharacteristic(characteristic)
//        }

        private fun broadcastUpdate(str: String) {
            val mHandler : Handler = object : Handler(Looper.getMainLooper()){
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Toast.makeText(context,str,Toast.LENGTH_SHORT).show()
                }
            }
            mHandler.obtainMessage().sendToTarget()
        }

        private fun disconnectGattServer() {
            Log.d(TAG, "Closing Gatt connection")
            // disconnect and close the gatt
            if (bluetoothGatt != null) {
                if (context?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    } != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.i(TAG,"onCharacteristicChanged")
            if (characteristic != null) {
                readCharacteristic(characteristic)
            }
        }

        private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {

            val msg = characteristic.getStringValue(0)
            txtRead = msg
            isTxtRead = true

            Log.d(TAG, "read : $msg")
        }
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                Log.e(TAG, "Characteristic write unsuccessful, status: $status")
                disconnectGattServer()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d (TAG, "Characteristic read successfully" );
                if (characteristic != null) {
                    readCharacteristic(characteristic)
                };
            } else {
                Log.e( TAG, "Characteristic read unsuccessful, status: " + status);
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
                // set to allow this. Normally this would be an error and you would want to:
                // disconnectGattServer();
            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun connectGatt(device:BluetoothDevice): BluetoothGatt? {
        this.device = device

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
            }
            bluetoothGatt = device.connectGatt(context, false, gattCallback,
                BluetoothDevice.TRANSPORT_LE)
        }
        else {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }
        return bluetoothGatt
    }
}