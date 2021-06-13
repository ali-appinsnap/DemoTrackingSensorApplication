package com.example.demotrackingsensorapplication.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class RecognitionData(var actionType: Int=-1, var actionConfidence: Int=0, var actionName: String="") :
    Parcelable