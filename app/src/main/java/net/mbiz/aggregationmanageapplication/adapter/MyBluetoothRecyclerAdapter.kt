package net.mbiz.aggregationmanageapplication.adapter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import net.mbiz.aggregationmanageapplication.R

class MyBluetoothRecyclerAdapter(private val itemList: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<MyBluetoothRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.bluetooth_item,viewGroup,false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (ActivityCompat.checkSelfPermission(
                holder.itemView.context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        holder.bluetoothDeviceName.text = itemList.get(position).name
        if(isConnected(itemList.get(position))){
            holder.deviceStatus.visibility = View.VISIBLE
            holder.deviceStatus.text = "연결됌"
        }else{
            holder.deviceStatus.visibility = View.GONE
        }

        Log.i("블루투스 디바이스 : " , "======================================================================================")
        Log.i("블루투스 디바이스 : " , "" + itemList.get(position).name)
        Log.i("블루투스 디바이스 : " , "" + itemList.get(position).address)
        Log.i("블루투스 디바이스 : " , "" + itemList.get(position).fetchUuidsWithSdp())
        Log.i("블루투스 디바이스 : " , "" + itemList.get(position).uuids.toString())
        Log.i("블루투스 디바이스 : " , "" + isConnected(itemList.get(position)))
        Log.i("블루투스 디바이스 : " , "======================================================================================")
        // 아래부터 추가코드
        if(myListener!=null){
            holder?.infoLayout?.setOnClickListener{v->
                Log.i("mListener v : " , "=======================================================" + v)
                Log.i("mListener position : " , "==========================================================" +position)
                myListener?.onClick(v, position)
            }

            holder?.bondedDetBtn?.setOnClickListener {v->
                Log.i("mListener v : " , "=======================================================" + v)
                Log.i("mListener position : " , "==========================================================" +position)
                bondedListener?.onClick(v, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val infoLayout = view.findViewById<LinearLayout>(R.id.device_info_layout)
        val bluetoothDeviceName = view.findViewById<TextView>(R.id.bluetooth_device_name)
        val deviceStatus = view.findViewById<TextView>(R.id.device_state_text)
        var itemLine = view.findViewById<View>(R.id.tableLine)
        val bondedDetBtn = view.findViewById<ImageButton>(R.id.bonded_delete_btn)
    }

    var myListener : OnItemClickListener? = null
    var bondedListener : OnItemClickListener? = null
    interface OnItemClickListener{
        fun onClick(view: View, position: Int)
    }

    fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}