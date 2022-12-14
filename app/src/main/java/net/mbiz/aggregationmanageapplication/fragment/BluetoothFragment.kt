package net.mbiz.aggregationmanageapplication.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.mbiz.aggregationmanageapplication.R
import net.mbiz.aggregationmanageapplication.adapter.BluetoothRecyclerAdapter
import net.mbiz.aggregationmanageapplication.adapter.MyBluetoothRecyclerAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.bluetoothAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.bluetoothDeviceArr
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.devicesArr
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.listItems
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mDelimiter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mDevices
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mInputStream
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mOutputStream
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mPairedDeviceCount
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mRemoteDevice
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mSocket
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mWorkerThread
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.myReceiver
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.readBuffer
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.readBufferPositon
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.registrationclerViewAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.scanRecyclerViewAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.stateFilter
import net.mbiz.aggregationmanageapplication.receiver.BroadcastReceiver
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.reflect.Method
import java.util.*
import kotlin.concurrent.thread


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BluetoothFragment : Fragment() {

    var mContext:Context? = null

    private val REQUEST_ALL_PERMISSION=2
    private val REQUEST_ENABLE_BT=3
    private var noCycle = false
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private lateinit var bleOnOffBtn: Switch
    private lateinit var bluetoothImg: ImageView

    // BLE Gatt ????????????
    private var bleGatt: BluetoothGatt? = null

    private lateinit var registrationViewManager: RecyclerView.LayoutManager

    private lateinit var scanViewManager: RecyclerView.LayoutManager

    private var scanning: Boolean = false

    private val scanHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bluetooth, container,false)

        if (context?.let {
                ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_SCAN) } != PackageManager.PERMISSION_GRANTED) { }

        bleOnOffBtn = view.findViewById<Switch>(R.id.ble_on_off_btn)
        bluetoothImg = view.findViewById<ImageView>(R.id.bluetooth_img_view)

        mContext = view.context

        recevierSet()

        // ???????????? ??????????????? ???????????? ??????, ???????????? ????????? ?????? ?????? switch ?????? ?????? ??????
        if(bluetoothAdapter!=null){
            // Device doesn't support Bluetooth
            if(bluetoothAdapter?.isEnabled==false){
                bleOnOffBtn.isChecked = false
            } else{
                bleOnOffBtn.isChecked = true

                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

//                mDevices = bluetoothAdapter!!.bondedDevices
//                Log.i("HC06 bondied Device : ","device size : " + ((mDevices as MutableSet<BluetoothDevice>?)?.size))
//                for (device in (mDevices as MutableSet<BluetoothDevice>?)!!) {
//                    Log.i("HC06 bondied Device : ","device : " + device.name)
//                    listItems.add(device)
//                }

//                scanDevice(true)
            }
            Log.i("???????" , "================================================================ ??????1")
            // ???????????? ????????? ????????? ?????? ?????? ?????????
            bluetoothImgchangeColor(bleOnOffBtn,bluetoothImg)
        }

        // ???????????? ?????? ?????? ?????? ?????????
        bleOnOffBtn.setOnCheckedChangeListener { _, isChecked ->
            Log.i("noCycle " , "================================================" + noCycle)
            /** boolean ?????? noCycle??? ?????? ?????? ->
             *  ??????????????? ????????? ??? ????????? ?????? ??? ???, ?????? ??? ???????????? ?????????????????? ????????????,
             *  ????????? ???????????? ????????? ????????? bluetoothOnOff ???????????? ??? ??? ???????????? ??? ???????????? ?????? **/
            if(!noCycle){
                // ???????????? ????????? ???????????? ?????????
                bluetoothOnOff()
                // ???????????? ????????? ?????? ?????? ?????????
            }else{
                noCycle = false
            }
            // ???????????? ????????? ????????? ?????? ?????? ?????????
            bluetoothImgchangeColor(bleOnOffBtn,bluetoothImg)
        }

        // ?????? ?????????????????? ??????
        scanViewManager = LinearLayoutManager(context)
        scanRecyclerViewAdapter = BluetoothRecyclerAdapter(bluetoothDeviceArr)

        // ?????? ??????????????????
        val rvScanBluetooth = view.findViewById<RecyclerView>(R.id.rv_scan_bluetooth).apply {
            layoutManager = scanViewManager
            adapter = scanRecyclerViewAdapter
        }

        // ????????? ?????? ?????? ??? ?????? ?????? ??? ???????????? ?????????
        scanRecyclerViewAdapter.mListener = object : BluetoothRecyclerAdapter.OnItemClickListener{
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onClick(view: View, position: Int) {

                if (context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }

                var device : BluetoothDevice = bluetoothDeviceArr.get(position)
                Log.d("Bluetooth Connect", device.getName());
                ConnectBluetoothDevice(device);  //???????????? ???????????? ????????? ???????????? ?????? ??????
                Log.d("Bluetooth Connect", "ConnectBluetoothDevice");

            }
        }


        registrationViewManager = LinearLayoutManager(context)
        registrationclerViewAdapter = MyBluetoothRecyclerAdapter(listItems)

        // ?????? ??????????????????
        val rvBluetooth = view.findViewById<RecyclerView>(R.id.rv_bluetooth).apply {
            layoutManager = registrationViewManager
            adapter = registrationclerViewAdapter
        }

        // ?????? ?????? ?????? ?????????
        registrationclerViewAdapter.bondedListener = object : MyBluetoothRecyclerAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
                var bondingDevice : BluetoothDevice = listItems[position]

                try {
                    var start = true
                        var sucessCheck : Boolean = bondingDevice::class.java.getMethod("removeBond").invoke(bondingDevice) as Boolean
                        bondingList()
                        start = sucessCheck

                } catch (e: Exception) {
                    Log.e("bluetoothFragment", "Removing bond has been failed. ${e.message}")
                }
            }
        }

        // ?????? ?????? ?????? ??? ?????? ?????????
        registrationclerViewAdapter.myListener = object : MyBluetoothRecyclerAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
                if (context?.let { ActivityCompat.checkSelfPermission(it.applicationContext, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }
                connectToSelectedDevice(listItems[position].name.toString())
                Log.i("MyBluetooth ?????? ?????????", " ????????????")
            }
        }
        scanRecyclerViewAdapter.notifyDataSetChanged()

        return view

    }

    // Scan??? ???????????? ?????? ??????
    fun ConnectBluetoothDevice(device: BluetoothDevice) {
        if (mContext?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }
        mDevices = bluetoothAdapter?.getBondedDevices()
        mPairedDeviceCount = mDevices!!.size

        //pairing?????? ?????? ????????? ????????? ???????????? ??????????????? ?????? ????????? ?????? ?????? ????????? ????????? ??????
        var already_bonded_flag = false
        if (mPairedDeviceCount > 0) {
            for (bonded_device in mDevices!!) {
                if (device.name.equals(bonded_device.name)) {
                    already_bonded_flag = true
                }
            }
        }

        //pairing process
        //?????? pairing????????? ????????? ?????? ????????? ????????????, ????????? createBond()????????? ????????? pairing??? ????????????.
        if (!already_bonded_flag) {
            try {
                //pairing??????
                device.createBond()

                bondingList()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } else {
            connectToSelectedDevice(device.name)
        }
    }

    // BoradcastReceiver ?????? ?????????
    private fun recevierSet() {
        stateFilter = IntentFilter()
        myReceiver = BroadcastReceiver()
        stateFilter!!.addAction(BluetoothAdapter.ACTION_STATE_CHANGED) //BluetoothAdapter.ACTION_STATE_CHANGED : ???????????? ???????????? ??????

        stateFilter!!.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        stateFilter!!.addAction(BluetoothDevice.ACTION_ACL_CONNECTED) //?????? ??????
        stateFilter!!.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) //?????? ?????? ??????

        stateFilter!!.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        stateFilter!!.addAction(BluetoothDevice.ACTION_FOUND) //?????? ?????????

        stateFilter!!.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) //?????? ?????? ??????

        stateFilter!!.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //?????? ?????? ??????

        stateFilter!!.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
    }

    private fun myBluetoothListValidation() {

    }

    private fun bluetoothImgchangeColor(bleOnOffBtn : Switch, bluetoothImg : ImageView) {
        // ???????????? ????????? ?????? ?????? ?????????
        if(bleOnOffBtn!!.isChecked){
            if (bluetoothImg != null) {
                bluetoothImg.setColorFilter(Color.parseColor("#AAC4FF"))
            }
        } else {
            if (bluetoothImg != null) {
                bluetoothImg.setColorFilter(Color.parseColor("#808080"))
            }
        }
    }

    private fun bluetoothOnOff() {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter","Device doesn't support Bluetooth")
        }else{
            if (bluetoothAdapter?.isEnabled == false) { // ???????????? ?????? ????????? ???????????? ?????????
                Log.i("bluetoothAdapter?" , "================================================================ false")
                Log.i("bluetoothAdapter?" , "================================================================ " + REQUEST_ENABLE_BT)

                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

//                bluetoothAdapter?.startDiscovery()
                registrationclerViewAdapter.notifyDataSetChanged()

            } else{ // ???????????? ??????????????? ???????????? ????????????
                Log.i("bluetoothAdapter?" , "================================================================ true")
                Log.i("bluetoothAdapter?" , "================================================================ " + REQUEST_ENABLE_BT)
                if (view?.let {
                    Log.i(" if (view?.let {", "??????")
                    ActivityCompat.checkSelfPermission(it.context, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }

                    listItems.clear()
                    stopScanDevice(true)
                    registrationclerViewAdapter.notifyDataSetChanged()
                    bluetoothAdapter?.disable()
            }
        }
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    // Permission ??????
    /**????????? Permission??? PERMISSIONS?????? ????????? ????????? ?????? ???,
      ?????? ????????? ????????? Permission??? ?????? ????????????. Bluetooth Scan ????????? ??????????????? ACCESS_FINE_LOCATION????????? ?????? ?????? Permission??? ?????????????????????.**/
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i( "REQUEST_ALL_PERMISSION", REQUEST_ALL_PERMISSION.toString())
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(context, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ???????????? ????????? ?????? ?????? ?????????
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i("requestCode "," =========================================== " + requestCode)
        Log.i("resultCode "," =========================================== " + resultCode)
        Log.i("RESULT_OK "," =========================================== " + RESULT_OK)

        if(requestCode == 3){
            if(resultCode == RESULT_OK) {
                // ???????????? ????????? ???????????? ??????
                if(!hasPermissions(context, PERMISSIONS)) {
                    requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
                }

                // permission ?????? ??????
                if (mContext?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }
                scanDevice(true)
                bluetoothAdapter?.startDiscovery()

                Log.i("HC06 bondied Device : ","device size : " + ((mDevices as MutableSet<BluetoothDevice>?)?.size))
                bondingList()

                registrationclerViewAdapter.notifyDataSetChanged()
                //
                Log.i("RESULT_OK "," =========================================== " + RESULT_OK)
                Log.i("requestCode "," =========================================== " + requestCode)
                Log.i("resultCode "," =========================================== " + resultCode)
            }else{
                Log.i("RESULT_NO "," =========================================== " + RESULT_OK)
                Log.i("requestCode "," =========================================== " + requestCode)
                Log.i("resultCode "," =========================================== " + resultCode)
                noCycle = true
                bleOnOffBtn.isChecked = false
            }
        }
    }

    private fun bondingList() {
        if (context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { }
        mDevices = bluetoothAdapter!!.bondedDevices
        listItems.clear()
        for (device in (mDevices as MutableSet<BluetoothDevice>?)!!) {
            Log.i("HC06 bondied Device : ","device : " + device.name)
            listItems.add(device)
        }
        registrationclerViewAdapter.notifyDataSetChanged()
    }

//    /** ???????????? ?????? ?????????
//     *  Scan??? ????????? ?????? OnScanFailed ?????????
//     *  Batch Scan Result??? ????????? ??? ???????????? onBatchScanResult ?????????
//     *  ???????????? ????????? ??????????????? ??? ???????????? onSCanResult ????????? ??? ??????????????? ?????????. **/
//    private val mLeScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    object: ScanCallback() {
//        override fun onScanFailed(errorCode: Int) {
//            super.onScanFailed(errorCode)
//            Log.d("scanCallback", "BLE Scan Failed : " + errorCode)
//        }
//
//        override fun onBatchScanResults(results: MutableList<ScanResult> ?) {
//            super.onBatchScanResults(results)
//            results?.let {
//                // results is not null
//                for(result in it) {
//                    if (context?.let { it1 ->
//                            ActivityCompat.checkSelfPermission(
//                                it1,
//                                Manifest.permission.BLUETOOTH_CONNECT
//                            )
//                        } != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        return
//                    }
//                    if(!devicesArr.contains(result.device) && result.device.name!=null) devicesArr.add(result.device)
//                }
//            }
//        }
//        override fun onScanResult(callbackType: Int, result: ScanResult?) {
//            super.onScanResult(callbackType, result)
//            result?.let {
//                // result is not null
//                if (context?.let { it1 ->
//                        ActivityCompat.checkSelfPermission(
//                            it1,
//                            Manifest.permission.BLUETOOTH_CONNECT
//                        )
//                    } != PackageManager.PERMISSION_GRANTED
//                ) {
//                    return
//                }
//                if(!devicesArr.contains(it.device) && it.device.name!=null) {
//                    if(bluetoothAdapter != null){
//                        val scanCheckbondedDevice : Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
//                        for (device in scanCheckbondedDevice!!){
//                            Log.i("scanCheckbondedDevice", "result.device.name = " + result.device.name)
//                            Log.i("scanCheckbondedDevice", "bonded.device.name = " + device.name)
//                            if(device.name.contains(result.device.name)){
//                                return
//                            }
//                        }
//                        devicesArr.add(result.device)
//                    }
//                }
//                scanRecyclerViewAdapter.notifyDataSetChanged()
//            }
//        }
//    }

    private fun scanDevice(state:Boolean){

        if(bluetoothDeviceArr.size == 0){
            Toast.makeText(activity,"There is no device", Toast.LENGTH_SHORT).show();
        }

        for( device in bluetoothDeviceArr){
            devicesArr.add(device)
            Log.i("Find Device " , "======================================================  " + device.name)
        }
    }

//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    private fun startDevice(state: Boolean) {
//        scanning = true
//        devicesArr.clear()
//
//        if (context?.let {
//                ActivityCompat.checkSelfPermission(
//                    it,
//                    Manifest.permission.BLUETOOTH_SCAN
//                )
//            } != PackageManager.PERMISSION_GRANTED
//        ) {
//        }
//        Log.i("startDevice : " , "==================== startDevice")
//        bluetoothAdapter?.bluetoothLeScanner?.startScan(mLeScanCallback)
//    }

    private  fun stopScanDevice(state: Boolean){
        Log.i("scanning : " , "==================== else")
        scanning = state
        if (context?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
        }
        bluetoothDeviceArr.clear()
        devicesArr.clear()
        scanRecyclerViewAdapter.notifyDataSetChanged()
    }

    private fun connectToSelectedDevice(selectedDeviceName: String) {
        //???????????? ????????? ???????????? ????????? ????????? ????????? ????????? ?????? ????????? ????????? ?????? GUI??? ????????? ?????????
        //????????? ?????? ????????? thread??? ???????????? thread??? ?????? ????????? ?????? ?????? ???????????? ????????????.
        Log.i("connectToSelectedDevice", "selectedDeviceName : " + selectedDeviceName)
        //handler??? thread?????? ????????? ???????????? ?????? ?????? ????????? ???????????????.
        val mHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what === 1) // ?????? ??????
                {
                    try {
                        Log.i("connectToSelectedDevice", " msg.what === 1 ")
                        registrationclerViewAdapter.notifyDataSetChanged()
                        scanRecyclerViewAdapter.notifyDataSetChanged()
                        //????????? ???????????? ???????????? outstream??? inputstream??? ?????????. ??????????????? ??????
                        //???????????? ?????? ?????? ????????? ??????.
                        mOutputStream = mSocket?.getOutputStream()
                        mInputStream = mSocket?.getInputStream()
                        // ????????? ?????? ??????
                        beginListenForData()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {    //?????? ??????
                    Toast.makeText(mContext, "Please check the device", Toast.LENGTH_SHORT).show()
                    try {
                        mSocket?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        //??????????????? ????????? thread ??????
        val thread = Thread {
            //????????? ????????? ????????? ?????? bluetooth device??? object
            mRemoteDevice = getDeviceFromBondedList(selectedDeviceName)
            Log.i("bluetoothConnectThread", " mRemoteDevice : " + mRemoteDevice)
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            try {
                // ?????? ??????
                mSocket = mRemoteDevice!!.createRfcommSocketToServiceRecord(uuid)
                Log.i("bluetoothConnectThread", " createRfcommSocketToServiceRecord ????????? ?????? ")
                // RFCOMM ????????? ?????? ??????, socket??? connect????????? ????????? ?????????. ????????? ui??? ????????? ?????? ?????? ????????????
                // Thread??? ?????? ????????? ???????????? ??????.
                mSocket?.let { if (mContext?.let { it1 -> ActivityCompat.checkSelfPermission(it1, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return@Thread }
                    mSocket!!.connect() }
                Log.i("connectToSelectedDevice", " mHandler.sendEmptyMessage(1) ")
                mHandler.sendEmptyMessage(1)
            } catch (e: Exception) {
                // ???????????? ?????? ??? ?????? ??????
                Log.i("connectToSelectedDevice", " mHandler.sendEmptyMessage(-1) ")
                mHandler.sendEmptyMessage(-1)
            }
        }

        //?????? thread??? ????????????
        thread.start()
    }

    // ?????????????????? ???????????? ?????? ?????????
    fun getDeviceFromBondedList(name: String): BluetoothDevice? {
        Log.i("getDeviceFromBondedList", "getDeviceFromBondedList ?????????")
        var selectedDevice: BluetoothDevice? = null
        if (mContext?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { }
        mDevices = bluetoothAdapter?.getBondedDevices()
        //pair ???????????? ?????? ????????? ?????? ?????? ??????, ????????? ?????? device ??????

        for (device in mDevices!!) {
            if (name == device.name) {
                Log.i("getDeviceFromBondedList", "name ================================================ " + name)
                selectedDevice = device
                break
            }
        }
        return selectedDevice
    }

    //???????????? ????????? ?????? Listener
    protected fun beginListenForData() {
        val handler = Handler()
        readBuffer = ByteArray(1024) //  ?????? ??????
        readBufferPositon = 0 //   ?????? ??? ?????? ?????? ?????? ??????

        Log.i("beginListenForData", "beginListenForData ????????? ?????? ")
        // ????????? ?????? ?????????
        mWorkerThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val bytesAvailable = mInputStream!!.available()
                    if (bytesAvailable > 0) { //???????????? ????????? ??????
                        val packetBytes = ByteArray(bytesAvailable)
                        mInputStream!!.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == mDelimiter) {
                                val encodedBytes = ByteArray(readBufferPositon)
                                System.arraycopy(
                                    readBuffer,
                                    0,
                                    encodedBytes,
                                    0,
                                    encodedBytes.size
                                )
                                    val uscharSet = Charsets.UTF_8
                                val data = String(encodedBytes, uscharSet)
                                readBufferPositon = 0
                                handler.post {
                                    Log.i("beginListenForData : " , "data : " + data)
                                    //????????? ???????????? data ????????? string?????? ??????!! ??? ???????????? ???????????? ??????.
                                    val c_arr = data.toCharArray() // char ????????? ??????
                                    if (c_arr[0] == 'a') {
                                        if (c_arr[1] == '1') {

                                            //a1????????? ???????????? ??????????????? ???
                                        }
                                        if (c_arr[1] == '2') {

                                            //a2?????? ???????????? ?????? ????????? ???
                                        }
                                    }
                                }
                            } else {
                                readBuffer.get(readBufferPositon++).toByte()
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
        //????????? ?????? thread ??????
        mWorkerThread!!.start()
        Log.i("beginListenForData", "beginListenForData ????????? ??? ")
    }

    override fun onResume() {
        super.onResume()
        if(myReceiver != null && stateFilter != null) requireActivity().registerReceiver(myReceiver, stateFilter)
    }

    override fun onPause() {
        super.onPause()
        if(myReceiver != null && stateFilter != null) requireActivity().unregisterReceiver(myReceiver)
    }
}

