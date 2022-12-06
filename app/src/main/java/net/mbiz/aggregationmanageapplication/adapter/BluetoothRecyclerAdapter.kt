package net.mbiz.aggregationmanageapplication.adapter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import net.mbiz.aggregationmanageapplication.R
import net.mbiz.aggregationmanageapplication.data.BluetoothData

class BluetoothRecyclerAdapter(private val itemList: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<BluetoothRecyclerAdapter.ViewHolder>() {

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
        holder.bluetoothDelbtn.visibility = View.INVISIBLE

        // 아래부터 추가코드
        if(mListener!=null){
            holder?.itemView?.setOnClickListener{v->
                Log.i("mListener v : " , "=======================================================" + v)
                Log.i("mListener position : " , "==========================================================" +position)
                mListener?.onClick(v, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val bluetoothDeviceName = view.findViewById<TextView>(R.id.bluetooth_device_name)
        val bluetoothDelbtn = view.findViewById<ImageButton>(R.id.bonded_delete_btn)
    }

    var mListener : OnItemClickListener? = null
    interface OnItemClickListener{
        fun onClick(view: View, position: Int)
    }
}