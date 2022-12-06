package net.mbiz.barcodescanner

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.*
import net.mbiz.aggregationmanageapplication.MainActivity
import net.mbiz.aggregationmanageapplication.R
import net.mbiz.aggregationmanageapplication.adapter.BarcodeRecyclerAdapter
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.barcodeList
import net.mbiz.aggregationmanageapplication.databinding.ActivityScanBinding

import net.mbiz.aggregationmanageapplication.util.BackKeyHandler


class ScanActivity : AppCompatActivity(){

    private lateinit var binding : ActivityScanBinding
    private lateinit var capture: CaptureManager

    // 바코드 레이아웃
    private lateinit var decoratedBarcodeView: DecoratedBarcodeView
    private lateinit var barcodeAdapter: BarcodeRecyclerAdapter

    // 뒤로가기 키 핸들러
    private var BackKeyHandler : BackKeyHandler = BackKeyHandler(this)

    private val TAG = ScanActivity::class.java.simpleName
    // Scan 할 바코드 타입 리스트
    private val scanList = mutableListOf<String>()
    private var scanBarcodeNum : String? = null

    // 디스플레이 화면 변수
    private lateinit var display : DisplayMetrics
    var deviceWidth : Int = 0
    var deviceHeight : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("ScanActivity : " , "ScanActivity 실행")

        //Log.i("saved Data : " , TestData);

        // 상태바 없애기
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // 타이틀바 없애기
        val actionBar = supportActionBar;
        actionBar?.hide()

        scanBarcodeNum = intent.getStringExtra("number")?.toString()

        // recyclerView 가져오기
        val rv_barcode = findViewById<RecyclerView>(R.id.rv_main_barcode)
        // recyclerView 레이아웃 설정
        rv_barcode.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        // 뭔지 모르겠는데, 붙치나 안붙치나 똑같아서 붙침
        rv_barcode.setHasFixedSize(true)

        // 리사이클러뷰 어댑터를 커스텀한 어댑터로 연결함
        barcodeAdapter = BarcodeRecyclerAdapter(barcodeList)

        rv_barcode.adapter = barcodeAdapter

        barcodeAdapter.bListener = object : BarcodeRecyclerAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
                Log.w("barcodeAdapterListener", "클릭됌")
                barcodeList.removeAt(position)
                barcodeAdapter.notifyDataSetChanged()
            }
        }

        decoratedBarcodeView = findViewById(R.id.zxing_barcode_scanner)

        capture = CaptureManager(this, decoratedBarcodeView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode() //decode

        // 화면 전환 이벤트
        changeDisplay()
        // 뒤로가기 버튼 클릭 이벤트
        goMainClicked()
    }

    fun goMainClicked() {
        binding.goMainBtn.setOnClickListener {
            Log.i(TAG, "==================================================ScanActivity의 뒤로가기 버튼 눌림 =============================================================")
            startActivity(Intent(this, MainActivity::class.java))

            finish()
        }
    }

    // 기기 별 화면에 맞게 스캔하는 화면 크기를 조절해주는 메서드
    fun changeDisplay() {

        // 핸드폰 화면의 크기
        display = this.applicationContext?.resources?.displayMetrics!!
        deviceWidth = display?.widthPixels!!
        deviceHeight = display?.heightPixels!!

        when(resources.configuration.orientation) {
            // 세로 전환 메서드
            Configuration.ORIENTATION_PORTRAIT -> {
                //Toast.makeText(this, "세로 화면 전환", Toast.LENGTH_SHORT).show()
                decoratedBarcodeView.barcodeView.framingRectSize = Size((deviceWidth!!/1.3).toInt(),(deviceHeight!!/2.5).toInt())
            }
            // 가로 전환 메서드
            Configuration.ORIENTATION_LANDSCAPE -> {
                //Toast.makeText(this, "가로 화면 전환", Toast.LENGTH_SHORT).show()
                decoratedBarcodeView.barcodeView.framingRectSize = Size((deviceWidth!!/1.3).toInt(),(deviceHeight!!/1.1).toInt())
            }
            else -> {} // Configuration.ORIENTATION_UNDEFINED
        }
    }

    // 픽셀 값을 DP로 바꿔주는 메서드
    fun px2dp(px: Int, context: Context): Float {
        return px / ((context.resources.displayMetrics.densityDpi.toFloat()) / DisplayMetrics.DENSITY_DEFAULT)
    }

    override fun onResume() {
        super.onResume()
            capture.onResume()
        Log.i("onReSume " , " onPause ------------------------------------")
    }
    override fun onPause() {
        super.onPause()
        capture.onPause()

//        val mHandler = Handler()
//        mHandler.postDelayed(Runnable {
//            // 시간 지난 후 실행할 코드
//
//        }, 100000) // 0.5초후

        Log.i("onPause " , " onPause ------------------------------------")
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
        Log.i("onDestroy " , " onDestroy ------------------------------------")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        BackKeyHandler.onBackPressed()
    }
}