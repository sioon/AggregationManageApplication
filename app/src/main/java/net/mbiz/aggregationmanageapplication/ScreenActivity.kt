package net.mbiz.barcodescanner

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import net.mbiz.aggregationmanageapplication.data.BarcodeData
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.barcodeList
import net.mbiz.aggregationmanageapplication.data.Constants.Companion.scanCount
import net.mbiz.aggregationmanageapplication.databinding.ActivityScanBinding

class ScreenActivity : AppCompatActivity() {

    private lateinit var binding : ActivityScanBinding
    private lateinit var scanOptions: ScanOptions

    private val TAG = ScreenActivity::class.java.simpleName
    private val scanList = mutableListOf<String>()
    private var scanBarcodeNum : String? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanBarcodeNum = intent.getStringExtra("number")?.toString()

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
        Log.i("ScreenActivity : " , "ScreenActivity 실행")
        // 스캔하는 카메라를 설정해주기 위해 scanOption 객체를 따로 만들어줬다.
        scanOptions = ScanOptions()

        // 스캔할 바코드 타입 담아주기
        scanList.add(ScanOptions.EAN_13)
        scanList.add(ScanOptions.DATA_MATRIX)
        scanList.add(ScanOptions.CODE_128)
        scanList.add(ScanOptions.QR_CODE)

        scanOptions.captureActivity = ScanActivity::class.java
        //scanOptions.setPrompt("바코드를 스캔해주세요.") // 스캐너 하단 문구 지정 메서드
        scanOptions.setBeepEnabled(false) // 스캐너가 바코드를 스캔 할 때, 소리가 날지 안날지 결정하는 메서드
        scanOptions.setTorchEnabled(false) // 초기 스캐너 실행 할 때, 스캐너의 플래시가 켜져있을지, 꺼져있을지 결정하는 메서드 기본값은 false(꺼짐)이다.
        scanOptions.setOrientationLocked(false) //회전을 통해 스캐너의 회전 방향 전환 자금을 설정 할 수 있는 메서드이다.
        scanOptions.setCameraId(0) // 0은 후면, 1은 전면이며, 기본값은 후면이다.
        scanOptions.setDesiredBarcodeFormats(scanList)

        // 런처를 실행해주지않으면 카메라 화면이 제대로 나오지않고 검은 화면만 나온다.
        barcodeLancher.launch(scanOptions);

    }
    private fun onLancher(scanOptions : ScanOptions) {
        barcodeLancher.launch(scanOptions)
    }

    private val barcodeLancher = registerForActivityResult(
        ScanContract()
    ) { result : ScanIntentResult -> // 스캔된 결과
        if (result != null) {
            if (result.contents == null) {
                finish()

            } else {
                Log.d(TAG, "Scanned: "+  result.contents)
                Log.d(TAG, "Scanned: formatName ="+  result.formatName)

                // 스캔 되었으니 스캔화면을 재시작해준다.
                val intent = Intent(getApplicationContext(), ScreenActivity::class.java)
                    intent.putExtra("number", result.contents)
                    intent.putExtra("type", result.formatName)

                for(bd in barcodeList){
                    Log.i("barcodeList", "===============================================================================")
                    Log.i("barcodeList", "result.contents : " + result.contents)
                    Log.i("barcodeList", "bd : " + bd.barcodeNum.toString())
                    Log.i("barcodeList", "scanCount : " + scanCount)
                    Log.i("barcodeList", "===============================================================================")
                    if(result.contents.equals(bd.barcodeNum.toString())){
                        Log.i("barcodeList", "===============================================================================")
                        Log.i( "barcode if", "result.contents.equals(bd.barcodeNum.toString()")
                        barcodeList.remove(bd)
                        scanCount = 1
                        Log.i("barcodeList", "scanCount : " + scanCount)
                        Log.i("barcodeList", "===============================================================================")
                        break
                    }
                }

                if(scanCount == 0){
                    Log.i("barcodeList", "===============================================================================")
                    Log.i( "barcode if", "scanCount == 0 barcodeList.add(BarcodeData(result.contents))")
                    Log.i("barcodeList", "scanCount : " + scanCount)
                    Log.i("barcodeList", "===============================================================================")

                    barcodeList.add(BarcodeData(result.contents))
                }

                for(bd in barcodeList){
                    Log.i("barcodeList", "===============================================================================")
                    Log.i("barcodeList", "result.contents : " + result.contents)
                    Log.i("barcodeList", "bd : " + bd.barcodeNum.toString())
                    Log.i("barcodeList", "scanCount : " + scanCount)
                    Log.i("barcodeList", "===============================================================================")
                }

                scanCount = 0
                Log.i("barcodeText != null : " , "======================================================  barcodeText != null")

                finish(); //현재 액티비티 종료 실시
                overridePendingTransition(0, 0); //인텐트 애니메이션 없애기
                startActivity(intent); //현재 액티비티 재실행 실시
                overridePendingTransition(0, 0); //인텐트 애니메이션 없애기
            }
        } else {
            Log.d(TAG, "스캔된 결과 없음.")
        }
    }
}