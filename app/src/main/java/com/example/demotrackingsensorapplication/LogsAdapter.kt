package com.example.demotrackingsensorapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.demotrackingsensorapplication.databinding.LogItemBinding

/**
 * Created by Muhammad Ali on 29-Apr-20.
 * Email muhammad.ali9385@gmail.com
 */
class LogsAdapter(
    val tripItems: ArrayList<String>
) :
    RecyclerView.Adapter<LogsAdapter.LogItemViewHolder>() {

    companion object {
        const val VIEW_SENDER = 1
        const val VIEW_RECEIVER = 2
    }

    var bindingSender: LogItemBinding? = null

    inner class LogItemViewHolder(var view: LogItemBinding) :
        RecyclerView.ViewHolder(view.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogItemViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        bindingSender =
            DataBindingUtil.inflate<LogItemBinding>(
                inflater,
                R.layout.log_item,
                parent,
                false
            )
        return LogItemViewHolder(bindingSender!!)
    }

    override fun getItemCount(): Int = tripItems.size


    fun updateList(tripItems: ArrayList<String>) {
        this.tripItems.clear()
        this.tripItems.addAll(tripItems)
        notifyDataSetChanged()
    }

    fun insertItem(tripItem: String) {
//        this.regChatItem.clear()
        this.tripItems.add(tripItem)
        notifyItemInserted(tripItems.size - 1)
    }


    override fun onBindViewHolder(holder: LogItemViewHolder, position: Int) {
        holder.view.logItem = tripItems[position]
//        holder.view.onViewClick = this
    }
}