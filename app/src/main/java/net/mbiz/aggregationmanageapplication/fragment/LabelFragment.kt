package net.mbiz.aggregationmanageapplication.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.mbiz.aggregationmanageapplication.R

class LabelFragment : PreferenceFragmentCompat() {

    var barcodeTypeListPreference : Preference? = null
    lateinit var prefs: SharedPreferences
    private val label_list : String = "label_list"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_set, rootKey)

        barcodeTypeListPreference = findPreference(label_list)

        prefs = PreferenceManager.getDefaultSharedPreferences(this.requireContext())

        if(!prefs.getString(label_list, "").equals("")){
            barcodeTypeListPreference?.setSummary(prefs.getString(label_list, "SSCC"))
        }

        prefs.registerOnSharedPreferenceChangeListener(prefListener)

    }

    val prefListener = SharedPreferences.OnSharedPreferenceChangeListener{
        sharedPreferences : SharedPreferences?, key: String? ->
        // key는 xml에 등록된 key에 해당
        if(key == label_list){
            Log.i("LabelFragment", "key : " + key)
            val choiceLabel = sharedPreferences!!.getString("label_list", "")
            barcodeTypeListPreference?.setSummary(choiceLabel)
        }
    }
}
