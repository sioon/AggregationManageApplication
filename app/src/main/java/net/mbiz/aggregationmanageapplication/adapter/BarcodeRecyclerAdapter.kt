package net.mbiz.aggregationmanageapplication.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.mbiz.aggregationmanageapplication.R
import net.mbiz.aggregationmanageapplication.data.BarcodeData
import net.mbiz.aggregationmanageapplication.databinding.BarcodeItemBinding

class BarcodeRecyclerAdapter(private val itemList: ArrayList<BarcodeData>) :
    RecyclerView.Adapter<BarcodeRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.barcode_item, viewGroup,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.barcodeNum.text = itemList.get(position).barcodeNum

        // 아래부터 추가코드
        if(bListener!=null){
            holder?.delBarcodeBtn?.setOnClickListener{v->
                Log.i("mListener v : " , "=======================================================" + v)
                Log.i("mListener position : " , "==========================================================" +position)
                bListener?.onClick(v, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val barcodeNum = view.findViewById<TextView>(R.id.barcode_text)
        var delBarcodeBtn = view.findViewById<ImageButton>(R.id.del_scan_barcode)
    }

    var bListener : OnItemClickListener? = null
    interface OnItemClickListener{
        fun onClick(view: View, position: Int)
    }
}