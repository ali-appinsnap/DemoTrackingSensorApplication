package com.example.demotrackingsensorapplication.eventBus

import android.content.Context
import android.content.Intent

interface ArrivedObjectListener {
    fun getIntent(context: Context?, intent: Intent?)
}