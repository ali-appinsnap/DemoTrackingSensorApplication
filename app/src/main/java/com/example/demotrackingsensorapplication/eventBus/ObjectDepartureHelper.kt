package com.example.demotrackingsensorapplication.eventBus

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import com.example.demotrackingsensorapplication.APP_TAG
import com.example.demotrackingsensorapplication.eventBus.ObjectBus.Companion.FILTER_ACTION

class ObjectDepartureHelper(var context: Context) {
    var intent: Intent = Intent(FILTER_ACTION)
    fun sendReInit() {
        context.sendBroadcast(intent)
    }

    fun sendStringBroadcast(intentKey: String, obj: String) {
        intent.putExtra(intentKey, obj)
        context.sendBroadcast(intent)
        Log.d(APP_TAG, obj)
    }

    fun sendIntBroadcast(intentKey: String, obj: Int) {
        intent.putExtra(intentKey, obj)
        context.sendBroadcast(intent)
        Log.d(APP_TAG, "" + obj)
    }

    fun sendLongBroadcast(intentKey: String, obj: Long) {
        intent.putExtra(intentKey, obj)
        context.sendBroadcast(intent)
        Log.d(APP_TAG, "" + obj)
    }

    fun sendDoubleBroadcast(intentKey: String, obj: Double) {
        intent.putExtra(intentKey, obj)
        context.sendBroadcast(intent)
        Log.d(APP_TAG, "" + obj)
    }

    public fun sendParcelableBroadcast(intentKey: String, obj: Any) {
        if (obj is Parcelable) {
            this.intent.putExtra(intentKey, obj)
            context.sendBroadcast(this.intent)
        }
    }

}