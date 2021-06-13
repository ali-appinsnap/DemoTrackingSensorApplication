package com.example.demotrackingsensorapplication.models

import android.location.Location
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TripLogHelper(
    var currentLocation: Location? = null,
    var previousLocation: Location? = null,
    var distanceBetween: Float = 0F,
    var speedDifference: Float = 0F,
    var bearingDifference: Float = 0F,
    var directionBetween: Float = 0F,
    var tripStarted: Boolean = false,
    var tripDistance: Double = 0.0,
    var logMsg: String = "",
    var recognitionData: RecognitionData = RecognitionData(),
    var tripStats: TripStats = TripStats()
) : Parcelable