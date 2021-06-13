package com.example.demotrackingsensorapplication.models

import android.os.Parcelable
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.android.parcel.Parcelize

@Parcelize
class TripStats(
    var maxSpeed: Float = 0f,//KM/H
    var isOverSpeeding: Boolean = false,
    var overSpeedCounter: Int = 0,
    var overSpeedThresholds: Float = 50f,//KM/H
    var isAccelerating: Boolean = false,
    var accelerationCounter: Int = 0,
    var accelerationThreshold: Int = 10,//KM/H
    var isHarshBreaking: Boolean = false,
    var harshBreakingCounter: Int = 0,
    var harshBreakingThreshold: Int = 30,//KM/H
    var isCornering: Boolean = false,
    var corneringCounter: Int = 0,
    var corneringThreshold: Int = 45
) : Parcelable