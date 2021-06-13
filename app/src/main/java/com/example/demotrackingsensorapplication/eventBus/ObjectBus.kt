package com.example.demotrackingsensorapplication.eventBus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ObjectBus(var arrivedObjectListener: ArrivedObjectListener? = null) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (arrivedObjectListener != null) {
            arrivedObjectListener!!.getIntent(context, intent)
        }
    }

    companion object {
        const val FILTER_ACTION = "ObjectBus"
    }
}