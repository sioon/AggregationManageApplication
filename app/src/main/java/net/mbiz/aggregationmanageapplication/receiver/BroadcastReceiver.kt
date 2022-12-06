package net.mbiz.aggregationmanageapplication.receiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import net.mbiz.aggregationmanageapplication.data.Constants
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.bluetoothDeviceArr
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.bluetoothDeviceCheck
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.listItems
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mInputStream
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mOutputStream
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.overlapCheck
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.registrationclerViewAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.scanRecyclerViewAdapter
import java.io.IOException
import java.io.UnsupportedEncodingException

class BroadcastReceiver : android.content.BroadcastReceiver() {
    var context: Context? = null
    var intent: Intent? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context
        this.intent = intent

        bluetoothStateReceiver()
    }

    private fun bluetoothStateReceiver() {
        val action : String? = intent?.action
        bluetoothDeviceCheck = action
//        Toast.makeText(context, "받은 액션 : " + action, Toast.LENGTH_SHORT).show()
        Log.d("Bluetooth action", "=========================" + action!!)
        val device : BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        var name : String? = null
        if (context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }
        if (device != null) {
            name = device.getName();    //broadcast를 보낸 기기의 이름을 가져온다.
        }

        if(action == BluetoothAdapter.ACTION_STATE_CHANGED){
            // 블루투스 연결 상태
            val state : Int? = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if(state == BluetoothAdapter.STATE_OFF){

            }else if(state == BluetoothAdapter.STATE_TURNING_OFF){

            }else if(state == BluetoothAdapter.STATE_ON){

            }else if(state == BluetoothAdapter.STATE_TURNING_ON){

            }
         }else if (action == BluetoothDevice.ACTION_ACL_CONNECTED){
            registrationclerViewAdapter.notifyDataSetChanged()
             // 블루투스 기기 연결됐을 경우
//            if (name != null) {
//                connectToSelectedDevice(name)
//            }
        }else if(action == BluetoothDevice.ACTION_BOND_STATE_CHANGED){
            // 본딩되어있는 기기가 변경될 경우

        }else if(action == BluetoothDevice.ACTION_ACL_DISCONNECTED){
            // 블루투스 기기 연결이 끊어질 경우
            registrationclerViewAdapter.notifyDataSetChanged()
        }else if(action == BluetoothAdapter.ACTION_DISCOVERY_STARTED){
            // 블루투스 기기 검색 시작
        }else if(action == BluetoothDevice.ACTION_FOUND){
            // 기기가 검색 될 경우
            var deviceName : String? = device?.name
            var deviceAddress : String? = device?.address

            Log.d("ACTION_FOUND ", "DeviceName ================================= " + deviceName)

            if(deviceName != null && device != null && listItems.isNotEmpty()){

                    // 스캔된 기기 중복 체크
                    for (mdevice in bluetoothDeviceArr){
                            if(mdevice.name.equals(deviceName)){
                                overlapCheck = false
                                break
                            }
                    }

                    // Bonding 되어있는 기기 중복 체크
                    for(bonDevice in listItems){
                        if(bonDevice.name.equals(deviceName)){
                            overlapCheck = false
                            break
                        }
                    }
                    if(overlapCheck) bluetoothDeviceArr.add(device)
                    overlapCheck = true
                    scanRecyclerViewAdapter.notifyDataSetChanged()
            }
            }else if(action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED){
                // 블루투스 기기 검색 종료
        }else if(action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED){

        }else if(action == BluetoothDevice.ACTION_PAIRING_REQUEST){

        }
    }

    private fun connectToSelectedDevice(selectedDeviceName: String) {
        //블루투스 기기에 연결하는 과정이 시간이 걸리기 때문에 그냥 함수로 수행을 하면 GUI에 영향을 미친다
        //따라서 연결 과정을 thread로 수행하고 thread의 수행 결과를 받아 다음 과정으로 넘어간다.
        Log.i("connectToSelectedDevice", "selectedDeviceName : " + selectedDeviceName)
        //handler는 thread에서 던지는 메세지를 보고 다음 동작을 수행시킨다.
        val mHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what === 1) // 연결 완료
                {
                    try {

                        Log.i("connectToSelectedDevice", " msg.what === 1 ")
                        //연결이 완료되면 소켓에서 outstream과 inputstream을 얻는다. 블루투스를 통해
                        //데이터를 주고 받는 통로가 된다.
                        if(mOutputStream == null){
                            mOutputStream = Constants.mSocket?.getOutputStream()
                        }
                        if(mInputStream == null){
                            mInputStream = Constants.mSocket?.getInputStream()
                        }

                        // 데이터 수신 준비
                        beginListenForData()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {    //연결 실패
                    Toast.makeText(context, "Please check the device", Toast.LENGTH_SHORT).show()
                    try {
                        Constants.mSocket?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            //블루투스 데이터 수신 Listener
            protected fun beginListenForData() {
                val handler = Handler()
                Constants.readBuffer = ByteArray(1024) //  수신 버퍼
                Constants.readBufferPositon = 0 //   버퍼 내 수신 문자 저장 위치

                Log.i("beginListenForData", "beginListenForData 메서드 시작 ")
                // 문자열 수신 쓰레드
                Constants.mWorkerThread = Thread {
                    while (!Thread.currentThread().isInterrupted) {
                        try {
                            val bytesAvailable = Constants.mInputStream!!.available()
                            if (bytesAvailable > 0) { //데이터가 수신된 경우
                                val packetBytes = ByteArray(bytesAvailable)
                                Constants.mInputStream!!.read(packetBytes)
                                for (i in 0 until bytesAvailable) {
                                    val b = packetBytes[i]
                                    if (b == Constants.mDelimiter) {
                                        val encodedBytes = ByteArray(Constants.readBufferPositon)
                                        System.arraycopy(
                                            Constants.readBuffer,
                                            0,
                                            encodedBytes,
                                            0,
                                            encodedBytes.size
                                        )
                                        val uscharSet = Charsets.UTF_8
                                        val data = String(encodedBytes, uscharSet)
                                        Constants.readBufferPositon = 0
                                        handler.post {
                                            Log.i("beginListenForData : " , "data : " + data)
                                            //수신된 데이터는 data 변수에 string으로 저장!! 이 데이터를 이용하면 된다.
                                            val c_arr = data.toCharArray() // char 배열로 변환
                                            if (c_arr[0] == 'a') {
                                                if (c_arr[1] == '1') {

                                                    //a1이라는 데이터가 수신되었을 때
                                                }
                                                if (c_arr[1] == '2') {

                                                    //a2라는 데이터가 수신 되었을 때
                                                }
                                            }
                                        }
                                    } else {
                                        Constants.readBuffer.get(Constants.readBufferPositon++).toByte()
                                    }
                                }
                            }
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                //데이터 수신 thread 시작
                Constants.mWorkerThread!!.start()
                Log.i("beginListenForData", "beginListenForData 메서드 끝 ")
            }
        }
    }
}