package net.mbiz.aggregationmanageapplication.fragment

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import net.mbiz.aggregationmanageapplication.R
import net.mbiz.aggregationmanageapplication.adapter.BluetoothRecyclerAdapter
import net.mbiz.aggregationmanageapplication.adapter.MyBluetoothRecyclerAdapter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class Hc06BluetoothFragment : Fragment() {

    private val REQUEST_ALL_PERMISSION=2
    private var noCycle = false
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private lateinit var bleOnOffBtn: Switch
    private lateinit var bluetoothImg: ImageView
    private lateinit var bleScanBtn: Switch
    private lateinit var bluetoothScanImg: ImageView

    // BLE Gatt 추가하기
    private var bleGatt: BluetoothGatt? = null
    private var mContext:Context? = null

    private lateinit var registrationViewManager: RecyclerView.LayoutManager
    private lateinit var registrationclerViewAdapter : MyBluetoothRecyclerAdapter

    private lateinit var scanViewManager: RecyclerView.LayoutManager
    private lateinit var scanRecyclerViewAdapter : BluetoothRecyclerAdapter

    private var scanning: Boolean = false

    private var registrationArr = ArrayList<BluetoothDevice>()
    private var devicesArr = ArrayList<BluetoothDevice>()

    private val scanHandler by lazy { Handler(Looper.getMainLooper()) }

    val REQUEST_ENABLE_BT: Int = 3
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mDevices: Set<BluetoothDevice>? = null
    var mPairedDeviceCount: Int = 0
    var mRemoteDevice: BluetoothDevice? = null
    var readBufferPositon : Int = 0
    var mSocket: BluetoothSocket? = null
    var mInputStream: InputStream? = null
    var mOutputStream: OutputStream? = null
    var mWorkerThread: Thread? = null
    lateinit var readBuffer : ByteArray
    var mDelimiter: Byte = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bluetooth, container,false)
        bleOnOffBtn = view.findViewById<Switch>(R.id.ble_on_off_btn)
        bluetoothImg = view.findViewById<ImageView>(R.id.bluetooth_img_view)

        CheckBluetooth()

        return view
    }

    fun CheckBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            // 장치가 블루투스 지원하지 않는 경우
            Toast.makeText(activity, "Bluetooth no available", Toast.LENGTH_SHORT).show()
        } else {
            // 장치가 블루투스 지원하는 경우
            if (!mBluetoothAdapter!!.isEnabled()) {
                // 블루투스를 지원하지만 비활성 상태인 경우
                // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요첨
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requireActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                // 블루투스를 지원하며 활성 상태인 경우
                // 페어링된 기기 목록을 보여주고 연결할 장치를 선택.
                selectDevice()
            }
        }
    }

    private fun selectDevice() {
        //페어링되었던 기기 목록 획득
        mDevices = mBluetoothAdapter!!.bondedDevices
        //페어링되었던 기기 갯수
        mPairedDeviceCount = (mDevices as MutableSet<BluetoothDevice>?)?.size!!
        //Alertdialog 생성(activity에는 context입력)
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        //AlertDialog 제목 설정
        builder.setTitle("Select device")


        // 페어링 된 블루투스 장치의 이름 목록 작성
        val listItems: MutableList<String> = ArrayList()
        for (device in (mDevices as MutableSet<BluetoothDevice>?)!!) {
            listItems.add(device.name)
        }
        if (listItems.size == 0) {
            //no bonded device => searching
            Log.d("Bluetooth", "No bonded device")
        } else {
            Log.d("Bluetooth", "Find bonded device")
            // 취소 항목 추가
            listItems.add("Cancel")
            val items = listItems.toTypedArray<CharSequence>()
            builder.setItems(items, DialogInterface.OnClickListener { dialog, which ->

                //각 아이템의 click에 따른 listener를 설정
                val dialog_: Dialog = dialog as Dialog
                // 연결할 장치를 선택하지 않고 '취소'를 누른 경우
                if (which == listItems.size - 1) {
                    Toast.makeText(dialog_.getContext(), "Choose cancel", Toast.LENGTH_SHORT).show()
                } else {
                    //취소가 아닌 디바이스를 선택한 경우 해당 기기에 연결
                    connectToSelectedDevice(items[which].toString())
                }
            })
            builder.setCancelable(false) // 뒤로 가기 버튼 사용 금지
            val alert: AlertDialog = builder.create()
            alert.show() //alert 시작
        }
    }

    private fun connectToSelectedDevice(selectedDeviceName: String) {
        //블루투스 기기에 연결하는 과정이 시간이 걸리기 때문에 그냥 함수로 수행을 하면 GUI에 영향을 미친다
        //따라서 연결 과정을 thread로 수행하고 thread의 수행 결과를 받아 다음 과정으로 넘어간다.

        //handler는 thread에서 던지는 메세지를 보고 다음 동작을 수행시킨다.
        val mHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what === 1) // 연결 완료
                {
                    try {
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
                    Toast.makeText(activity, "Please check the device", Toast.LENGTH_SHORT).show()
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
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            try {
                // 소켓 생성
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
                    return@Thread
                }
                mSocket = mRemoteDevice?.createRfcommSocketToServiceRecord(uuid)
                // RFCOMM 채널을 통한 연결, socket에 connect하는데 시간이 걸린다. 따라서 ui에 영향을 주지 않기 위해서는
                // Thread로 연결 과정을 수행해야 한다.
                mSocket?.connect()
                mHandler.sendEmptyMessage(1)
            } catch (e: Exception) {
                // 블루투스 연결 중 오류 발생
                mHandler.sendEmptyMessage(-1)
            }
        }

        //연결 thread를 수행한다
        thread.start()
    }

    //기기에 저장되어 있는 해당 이름을 갖는 블루투스 디바이스의 bluetoothdevice 객채를 출력하는 함수
//bluetoothdevice객채는 기기의 맥주소뿐만 아니라 다양한 정보를 저장하고 있다.

    //기기에 저장되어 있는 해당 이름을 갖는 블루투스 디바이스의 bluetoothdevice 객채를 출력하는 함수
    //bluetoothdevice객채는 기기의 맥주소뿐만 아니라 다양한 정보를 저장하고 있다.
    fun getDeviceFromBondedList(name: String): BluetoothDevice? {
        var selectedDevice: BluetoothDevice? = null

        mDevices = mBluetoothAdapter!!.bondedDevices
        //pair 목록에서 해당 이름을 갖는 기기 검색, 찾으면 해당 device 출력
        for (device in (mDevices as MutableSet<BluetoothDevice>?)!!) {
            if (name == device.name) {
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

        // 문자열 수신 쓰레드
        mWorkerThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val bytesAvailable = mInputStream!!.available()
                    if (bytesAvailable > 0) { //데이터가 수신된 경우
                        val packetBytes = ByteArray(bytesAvailable)
                        mInputStream!!.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            var b = packetBytes[i]
                            if (b == mDelimiter) {
                                val encodedBytes = ByteArray(readBufferPositon)
                                System.arraycopy(
                                    readBuffer,
                                    0,
                                    encodedBytes,
                                    0,
                                    encodedBytes.size
                                )
                                val uscharSet = Charsets.US_ASCII
                                val data = String(encodedBytes, uscharSet)
                                readBufferPositon = 0
                                handler.post {
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

                                readBuffer.get(readBufferPositon++)
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
    }

    fun SendResetSignal() {
        val msg = "bs00000"
        try {
            mOutputStream!!.write(msg.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
