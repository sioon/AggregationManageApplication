package net.mbiz.aggregationmanageapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.mbiz.aggregationmanageapplication.databinding.ActivityPreferenceBinding
import net.mbiz.aggregationmanageapplication.fragment.BluetoothFragment
import net.mbiz.aggregationmanageapplication.fragment.Hc06BluetoothFragment
import net.mbiz.aggregationmanageapplication.fragment.LabelFragment

private const val TAG_BLUETOOTH = "bluetooth_fragment"
private const val TAG_LABEL = "label_fragment"

class PreferenceActivity : AppCompatActivity() {

    private lateinit var binding : ActivityPreferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 타이틀바 없애기
        val actionBar = supportActionBar;
        actionBar?.hide()

        setFragment(TAG_BLUETOOTH, BluetoothFragment())
        binding.navigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bluetooth_Fragment -> setFragment(TAG_BLUETOOTH, BluetoothFragment())
                R.id.label_Fragment -> setFragment(TAG_LABEL, LabelFragment())
            }
            true
        }

        binding.goMainBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setFragment(tag: String, fragment: Fragment) {
        val manager: FragmentManager = supportFragmentManager
        val fragmentTransaction = manager.beginTransaction()

        if(manager.findFragmentByTag(tag) == null){
            fragmentTransaction.add(R.id.fragment_view, fragment, tag)
        }

        val bluetooth = manager.findFragmentByTag(TAG_BLUETOOTH)
        val label = manager.findFragmentByTag(TAG_LABEL)

        if(bluetooth != null){
            fragmentTransaction.hide(bluetooth)
        }

        if(label != null){
            fragmentTransaction.hide(label)
        }

        if(tag == TAG_BLUETOOTH) {
            if(bluetooth != null) {
                fragmentTransaction.show(bluetooth)
            }
        }

        if(tag == TAG_LABEL) {
            if(label != null) {
                fragmentTransaction.show(label)
            }
        }

        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun onPause() {
        super.onPause()
    }
}