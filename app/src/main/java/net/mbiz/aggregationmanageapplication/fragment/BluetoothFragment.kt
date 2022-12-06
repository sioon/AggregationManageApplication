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

    // BLE Gatt 추가하기
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

        // 사용자의 블루투스가 켜져있는 경우, 꺼져있는 경우에 따라 시작 switch 버튼 상태 설정
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
            Log.i("흐름?" , "================================================================ 흐름1")
            // 블루투스 스위치 이미지 색상 변경 메서드
            bluetoothImgchangeColor(bleOnOffBtn,bluetoothImg)
        }

        // 블루투스 연결 버튼 클릭 이벤트
        bleOnOffBtn.setOnCheckedChangeListener { _, isChecked ->
            Log.i("noCycle " , "================================================" + noCycle)
            /** boolean 타입 noCycle을 만든 이유 ->
             *  블루투스를 연결할 때 권한을 허락 할 지, 안할 지 선택하는 다이어로그가 나오는데,
             *  권한을 허락하지 않았을 경우에 bluetoothOnOff 메소드가 두 번 실행되는 걸 막기위해 만듬 **/
            if(!noCycle){
                // 블루투스 활성화 비활성화 메서드
                bluetoothOnOff()
                // 블루투스 이미지 색상 변경 메서드
            }else{
                noCycle = false
            }
            // 블루투스 스위치 이미지 색상 변경 메서드
            bluetoothImgchangeColor(bleOnOffBtn,bluetoothImg)
        }

        // 검색 리사이클러뷰 설정
        scanViewManager = LinearLayoutManager(context)
        scanRecyclerViewAdapter = BluetoothRecyclerAdapter(bluetoothDeviceArr)

        // 검색 리사이클러뷰
        val rvScanBluetooth = view.findViewById<RecyclerView>(R.id.rv_scan_bluetooth).apply {
            layoutManager = scanViewManager
            adapter = scanRecyclerViewAdapter
        }

        // 검색된 기기 목록 중 기기 클릭 시 발생하는 이벤트
        scanRecyclerViewAdapter.mListener = object : BluetoothRecyclerAdapter.OnItemClickListener{
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onClick(view: View, position: Int) {

                if (context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }

                var device : BluetoothDevice = bluetoothDeviceArr.get(position)
                Log.d("Bluetooth Connect", device.getName());
                ConnectBluetoothDevice(device);  //해당하는 블루투스 객체를 이용하여 연결 시도
                Log.d("Bluetooth Connect", "ConnectBluetoothDevice");

            }
        }


        registrationViewManager = LinearLayoutManager(context)
        registrationclerViewAdapter = MyBluetoothRecyclerAdapter(listItems)

        // 등록 리사이클러뷰
        val rvBluetooth = view.findViewById<RecyclerView>(R.id.rv_bluetooth).apply {
            layoutManager = registrationViewManager
            adapter = registrationclerViewAdapter
        }

        // 삭제 버튼 클릭 이벤트
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

        // 등록 기기 클릭 시 발생 이벤트
        registrationclerViewAdapter.myListener = object : MyBluetoothRecyclerAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
                if (context?.let { ActivityCompat.checkSelfPermission(it.applicationContext, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }
                connectToSelectedDevice(listItems[position].name.toString())
                Log.i("MyBluetooth 클릭 메서드", " 응애응애")
            }
        }
        scanRecyclerViewAdapter.notifyDataSetChanged()

        return view

    }

    // Scan한 블루투스 기기 연결
    fun ConnectBluetoothDevice(device: BluetoothDevice) {
        if (mContext?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return }
        mDevices = bluetoothAdapter?.getBondedDevices()
        mPairedDeviceCount = mDevices!!.size

        //pairing되어 있는 기기의 목록을 가져와서 연결하고자 하는 기기가 이전 기기 목록에 있는지 확인
        var already_bonded_flag = false
        if (mPairedDeviceCount > 0) {
            for (bonded_device in mDevices!!) {
                if (device.name.equals(bonded_device.name)) {
                    already_bonded_flag = true
                }
            }
        }

        //pairing process
        //만약 pairing기록이 있으면 바로 연결을 수행하며, 없으면 createBond()함수를 통해서 pairing을 수행한다.
        if (!already_bonded_flag) {
            try {
                //pairing수행
                device.createBond()

                bondingList()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } else {
            connectToSelectedDevice(device.name)
        }
    }

    // BoradcastReceiver 관련 메서드
    private fun recevierSet() {
        stateFilter = IntentFilter()
        myReceiver = BroadcastReceiver()
        stateFilter!!.addAction(BluetoothAdapter.ACTION_STATE_CHANGED) //BluetoothAdapter.ACTION_STATE_CHANGED : 블루투스 상태변화 액션

        stateFilter!!.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        stateFilter!!.addAction(BluetoothDevice.ACTION_ACL_CONNECTED) //연결 확인
        stateFilter!!.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) //연결 끊김 확인

        stateFilter!!.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        stateFilter!!.addAction(BluetoothDevice.ACTION_FOUND) //기기 검색됨

        stateFilter!!.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) //기기 검색 시작

        stateFilter!!.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //기기 검색 종료

        stateFilter!!.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
    }

    private fun myBluetoothListValidation() {

    }

    private fun bluetoothImgchangeColor(bleOnOffBtn : Switch, bluetoothImg : ImageView) {
        // 블루투스 이미지 색상 변경 메서드
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
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                Log.i("bluetoothAdapter?" , "================================================================ false")
                Log.i("bluetoothAdapter?" , "================================================================ " + REQUEST_ENABLE_BT)

                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

//                bluetoothAdapter?.startDiscovery()
                registrationclerViewAdapter.notifyDataSetChanged()

            } else{ // 블루투스 켜져있으면 블루투스 비활성화
                Log.i("bluetoothAdapter?" , "================================================================ true")
                Log.i("bluetoothAdapter?" , "================================================================ " + REQUEST_ENABLE_BT)
                if (view?.let {
                    Log.i(" if (view?.let {", "실행")
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

    // Permission 확인
    /**요청할 Permission을 PERMISSIONS라는 이름의 배열로 저장 후,
      해당 배열에 저장된 Permission을 모두 요청한다. Bluetooth Scan 기능을 사용하려면 ACCESS_FINE_LOCATION이라는 위치 접근 Permission을 허용해줘야한다.**/
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

    // 블루투스 활성화 권한 허용 메서드
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i("requestCode "," =========================================== " + requestCode)
        Log.i("resultCode "," =========================================== " + resultCode)
        Log.i("RESULT_OK "," =========================================== " + RESULT_OK)

        if(requestCode == 3){
            if(resultCode == RESULT_OK) {
                // 블루투스 권한을 허용헀을 경우
                if(!hasPermissions(context, PERMISSIONS)) {
                    requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
                }

                // permission 권한 체크
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

//    /** 블루투스 스캔 메서드
//     *  Scan에 실패할 경우 OnScanFailed 메소드
//     *  Batch Scan Result가 전달될 때 콜백하는 onBatchScanResult 메소드
//     *  블루트스 기기가 발견되었을 때 실행되는 onSCanResult 메소드 를 오버라이드 해준다. **/
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
                        registrationclerViewAdapter.notifyDataSetChanged()
                        scanRecyclerViewAdapter.notifyDataSetChanged()
                        //연결이 완료되면 소켓에서 outstream과 inputstream을 얻는다. 블루투스를 통해
                        //데이터를 주고 받는 통로가 된다.
                        mOutputStream = mSocket?.getOutputStream()
                        mInputStream = mSocket?.getInputStream()
                        // 데이터 수신 준비
                        beginListenForData()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {    //연결 실패
                    Toast.makeText(mContext, "Please check the device", Toast.LENGTH_SHORT).show()
                    try {
                        mSocket?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        //연결과정을 수행할 thread 생성
        val thread = Thread {
            //선택된 기기의 이름을 갖는 bluetooth device의 object
            mRemoteDevice = getDeviceFromBondedList(selectedDeviceName)
            Log.i("bluetoothConnectThread", " mRemoteDevice : " + mRemoteDevice)
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            try {
                // 소켓 생성
                mSocket = mRemoteDevice!!.createRfcommSocketToServiceRecord(uuid)
                Log.i("bluetoothConnectThread", " createRfcommSocketToServiceRecord 메서드 실행 ")
                // RFCOMM 채널을 통한 연결, socket에 connect하는데 시간이 걸린다. 따라서 ui에 영향을 주지 않기 위해서는
                // Thread로 연결 과정을 수행해야 한다.
                mSocket?.let { if (mContext?.let { it1 -> ActivityCompat.checkSelfPermission(it1, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { return@Thread }
                    mSocket!!.connect() }
                Log.i("connectToSelectedDevice", " mHandler.sendEmptyMessage(1) ")
                mHandler.sendEmptyMessage(1)
            } catch (e: Exception) {
                // 블루투스 연결 중 오류 발생
                Log.i("connectToSelectedDevice", " mHandler.sendEmptyMessage(-1) ")
                mHandler.sendEmptyMessage(-1)
            }
        }

        //연결 thread를 수행한다
        thread.start()
    }

    // 본딩되어있는 블루투스 기기 리스트
    fun getDeviceFromBondedList(name: String): BluetoothDevice? {
        Log.i("getDeviceFromBondedList", "getDeviceFromBondedList 메서드")
        var selectedDevice: BluetoothDevice? = null
        if (mContext?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) } != PackageManager.PERMISSION_GRANTED) { }
        mDevices = bluetoothAdapter?.getBondedDevices()
        //pair 목록에서 해당 이름을 갖는 기기 검색, 찾으면 해당 device 출력

        for (device in mDevices!!) {
            if (name == device.name) {
                Log.i("getDeviceFromBondedList", "name ================================================ " + name)
                selectedDevice = device
                break
            }
        }
        return selectedDevice
    }

    //블루투스 데이터 수신 Listener
    protected fun beginListenForData() {
        val handler = Handler()
        readBuffer = ByteArray(1024) //  수신 버퍼
        readBufferPositon = 0 //   버퍼 내 수신 문자 저장 위치

        Log.i("beginListenForData", "beginListenForData 메서드 시작 ")
        // 문자열 수신 쓰레드
        mWorkerThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val bytesAvailable = mInputStream!!.available()
                    if (bytesAvailable > 0) { //데이터가 수신된 경우
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
        //데이터 수신 thread 시작
        mWorkerThread!!.start()
        Log.i("beginListenForData", "beginListenForData 메서드 끝 ")
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

