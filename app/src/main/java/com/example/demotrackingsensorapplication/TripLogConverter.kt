package com.example.demotrackingsensorapplication

import androidx.room.TypeConverter
import com.example.demotrackingsensorapplication.models.TripLogHelper
import com.google.gson.Gson
import java.lang.Exception
import java.lang.reflect.Type
import java.util.*


class TripLogConverter {

    var gson = Gson()

    @TypeConverter
    fun stringToTripLogHelper(data: String?): TripLogHelper? {
        if (data == null) {
            return null
        }
        return try {
            gson.fromJson(data, TripLogHelper::class.java)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun tripLogHelperToString(someObjects: TripLogHelper?): String? {
        if(someObjects==null){
            return null
        }
        return gson.toJson(someObjects)
    }

}