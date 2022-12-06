package net.mbiz.aggregationmanageapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.mbiz.aggregationmanageapplication.adapter.BarcodeRecyclerAdapter
import net.mbiz.aggregationmanageapplication.data.BarcodeData
import net.mbiz.aggregationmanageapplication.data.Constants
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.barcodeList
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.bluetoothAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.bluetoothDeviceCheck
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.mOutputStream
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.myReceiver
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.stateFilter
import net.mbiz.aggregationmanageapplication.databinding.ActivityMainBinding
import net.mbiz.aggregationmanageapplication.receiver.BroadcastReceiver
import net.mbiz.barcodescanner.ScreenActivity
import net.mbiz.mybluetoothlib.MyBluetooth
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var barcodeAdapter : BarcodeRecyclerAdapter
    private lateinit var aggregation : ImageView
    private lateinit var preferences : ImageView
    private val TAG = MainActivity::class.java.simpleName

    private val REQUEST_ENABLE_BT=1

    private var scanBarcodeNum : String? = null

    // 환경 설정 세팅
    private lateinit var labelType : String
    private var barcodeTypeListPreference : Preference? = null

    //바코드 세팅
    var gs1SerialNum = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothPermissionChecker()
        recevierSet()
        // 액션바 숨기기
        val ActionBar = supportActionBar
        ActionBar?.hide()

        // 상태바 삭제하기
        // window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        Log.d(TAG, "scanBarcodeNum ======================================= "+  scanBarcodeNum)

        // 환경 설정 세팅
        var sharedPreferences : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        labelType = sharedPreferences!!.getString("label_list", "").toString()

        if(labelType.equals("")){
            labelType = "SSCC"
        }


        Log.i(TAG,"labelType = " + labelType)

        if(!sharedPreferences.getString("label_list", "").equals("")){
            barcodeTypeListPreference?.setSummary(sharedPreferences.getString("label_list", "SSCC"))
        }

        // 바코드 RecyclerView 연결하는 부분
        val rv_barcode = findViewById<RecyclerView>(R.id.rv_main_barcode)
        rv_barcode.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false)
        rv_barcode.setHasFixedSize(true)
        barcodeAdapter = BarcodeRecyclerAdapter(barcodeList)
        rv_barcode.adapter = barcodeAdapter

        barcodeAdapter.bListener = object : BarcodeRecyclerAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
                Log.w("barcodeAdapterListener", "클릭됌")
                barcodeList.removeAt(position)
                barcodeAdapter.notifyDataSetChanged()
            }
        }

        // ScreenActivity로 이동하는 메서드
        goAggregaitionView()
        // 환경설정 버튼 클릭 이벤트 메서드
        goPreferencesView()
        // 바코드 스캔 버튼 클릭 메서드
        scanBarcodeAggregation()
        setData()
    }

    private fun scanBarcodeAggregation() {
        val aggregationBtn = binding.aggregationBtn

        aggregationBtn.setOnClickListener {
            Log.i(TAG,"포장하기 버튼 클릭")
            // 블루투스가 켜져있지 않은 경우
            if(bluetoothAdapter?.isEnabled == false){
                val overlapBarcodeCheckToast = Toast.makeText(this,"블루투스가 켜져있지않습니다.", Toast.LENGTH_SHORT)
                overlapBarcodeCheckToast.setGravity(Gravity.CENTER_VERTICAL,0,0)
                overlapBarcodeCheckToast.show()
                return@setOnClickListener
            }else

            if(bluetoothDeviceCheck != BluetoothAdapter.ACTION_DISCOVERY_FINISHED && bluetoothDeviceCheck != BluetoothDevice.ACTION_ACL_DISCONNECTED){
                // 기기가 연결되어있지 않은 경우
                val overlapBarcodeCheckToast = Toast.makeText(this,"프린터를 연결해주세요!", Toast.LENGTH_SHORT)
                overlapBarcodeCheckToast.setGravity(Gravity.CENTER_VERTICAL,0,0)
                overlapBarcodeCheckToast.show()
                return@setOnClickListener
            }

            if(bluetoothAdapter == null){
                // 기기가 연결되어있지 않은 경우
                val overlapBarcodeCheckToast = Toast.makeText(this,"프린터를 연결해주세요!", Toast.LENGTH_SHORT)
                overlapBarcodeCheckToast.setGravity(Gravity.CENTER_VERTICAL,0,0)
                overlapBarcodeCheckToast.show()

                return@setOnClickListener
            }
            if(barcodeList.size > 0){
                // 포장할 바코드 리스트가 있는 경우 프린터로 데이터 전송
                SendResetSignal()
            }else{
                // 포장할 바코드 리스트가 없는 경우 Toast메세지 알림
                val overlapBarcodeCheckToast = Toast.makeText(this,"스캔된 바코드가 없습니다. \n 바코드를 스캔해주세요.", Toast.LENGTH_SHORT)
                overlapBarcodeCheckToast.setGravity(Gravity.CENTER_VERTICAL,0,0)
                overlapBarcodeCheckToast.show()
                return@setOnClickListener
            }
        }
    }

    private fun bluetoothPermissionChecker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                REQUEST_ENABLE_BT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH
                ),
                REQUEST_ENABLE_BT
            )
        }
    }

    fun goAggregaitionView() {
        // 스캐너 화면으로 이동하는 버튼
        aggregation = findViewById(R.id.aggregationImageView)
        aggregation.setOnClickListener {
            Log.i("aggregationImageView" , " 버튼 눌림 ")
            startActivity(Intent(this, ScreenActivity::class.java))
            finish()
        }
    }

    fun goPreferencesView() {
        // 환경 설정으로 이동하는 버튼
        preferences = binding.preferencesImageView

        preferences.setOnClickListener{
            startActivity(Intent(this, PreferenceActivity::class.java))
            finish()
        }
    }

    fun setData() {
        Log.i(TAG, "setData =============================================================================== " + barcodeList.size)
        var listSText = findViewById<TextView>(R.id.list_size_text)
        listSText.setText("바코드 " + barcodeList.size.toString() + "개")
    }

    // 연결된 디바이스로 데이터 전송
    fun SendResetSignal() {
        val barcode = 188064190002038880

        try {
            if(labelType.equals("SSCC")){
                val sscc =  "^XA\n" +
                            "^MMT\n" +
                            "^PW831\n" +
                            "^LL0406\n" +
                            "^LS0\n" +
                            "^BY1,1,150^FT240,250^BCN,,N,N\n" +
                            "^FD>:00" + barcode +"^FS\n" +
                            "^FT100,320^A0N,40,50^FH\\^FD(00)"+barcode+"^FS\n" +
                            "^FT100,70^A0N,40,50^FH\\^FDSSCC^FS\n" +
                            "^PQ1,0,1,Y^XZ"

                mOutputStream!!.write(sscc.toByteArray())

            }
            if(labelType.equals("GS1")){
                val gtin = "18806980005719"
                var serialEng = "A"
                val serialNumSize = 5
                Log.i("labelType.equals(\"GS1\")" , "serialNum = " + gs1SerialNum)
                var serial = serialEng + gs1SerialNum.toString().padStart(serialNumSize,'0')
                Log.i("labelType.equals(\"GS1\")" , "serial = " + serial)
                val gs1 = "^XA\n" +
                        "^MMT\n" +
                        "^PW719\n" +
                        "^LL0360\n" +
                        "^LS0\n" +
                        "^BY1,3,150^FT200,250^BCN,150,N,N,,D\n" +
                        "^FD01" + gtin + "21" + serial + "^FS\n" +
                        "^FT100,320^A0N,40,35^FH\\^FD(01)" + gtin + "(21)" + serial + "^FS\n" +
                        "^PQ1,0,1,Y^XZ"
                mOutputStream!!.write(gs1.toByteArray())
                gs1SerialNum += 1
            }
        } catch (e: IOException) {
            e.printStackTrace()
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

    override fun onResume() {
        super.onResume()
        if(myReceiver != null && stateFilter != null) this.registerReceiver(myReceiver, stateFilter)
    }

    override fun onPause() {
        super.onPause()
        if(myReceiver != null && stateFilter != null) this.unregisterReceiver(myReceiver)
    }
}