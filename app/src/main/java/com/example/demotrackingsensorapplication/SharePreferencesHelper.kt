package com.example.demotrackingsensorapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.example.demotrackingsensorapplication.models.TripLogHelper
import com.example.demotrackingsensorapplication.services.ServiceState

/**
 * Created by Muhammad Ali on 19-May-20.
 * Email muhammad.ali9385@gmail.com
 */
class SharePreferencesHelper {

    companion object {
        private const val PREFS_TIME = "prefs_time"
        private const val AUTH_TOKEN = "Auth_token"
        private const val CUSTOMER_ID = "customer_id"
        private const val SCOPE_PASS = "scope_pass"
        private const val SCOPE_NAME = "scope_name"
        private const val TRIP_LOG = "tripLog"

        private const val SERVICE_STATE = "genixForegroundServiceState"
        private var prefs: SharedPreferences? = null

        @Volatile
        private var instance: SharePreferencesHelper? = null
        private var lock = Any()


        operator fun invoke(context: Context): SharePreferencesHelper =
            instance ?: kotlin.synchronized(lock) {
                instance ?: buildHelper(context).also {
                    instance = it
                }
            }

        private fun buildHelper(context: Context): SharePreferencesHelper {
            prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return SharePreferencesHelper()
        }
    }

    fun updateTime(time: Long) {
        prefs?.edit(commit = true) {
            putLong(PREFS_TIME, time)
        }
    }

    fun updateAuth(auth: String) {
        prefs?.edit(commit = true) {
            putString(AUTH_TOKEN, auth)
        }
    }

    fun updateScopeName(auth: String) {
        prefs?.edit(commit = true) {
            putString(SCOPE_NAME, auth)
        }
    }

    fun updateScopePass(auth: String) {
        prefs?.edit(commit = true) {
            putString(SCOPE_PASS, auth)
        }
    }

    fun updateTripLog(tripLogHelper: TripLogHelper) {
        val tripLogConverter = TripLogConverter()
        val tripLog = tripLogConverter.tripLogHelperToString(tripLogHelper)
        prefs?.edit(commit = true) {
            putString(TRIP_LOG, tripLog)
        }
    }

    fun updateCustomerId(id: Long) {
        prefs?.edit(commit = true) {
            putLong(CUSTOMER_ID, id)
        }
    }

    fun updateForegroundServiceState(state: ServiceState) {
        prefs?.edit(commit = true) {
            putString(SERVICE_STATE, state.name)
        }
    }

    fun getTime() = prefs?.getLong(PREFS_TIME, 0L)

    fun getAuth(): String = if (prefs != null) {
        prefs?.getString(AUTH_TOKEN, "")!!
    } else {
        ""
    }

    fun getScopeName(): String = if (prefs != null) {
        prefs?.getString(SCOPE_NAME, "")!!
    } else {
        ""
    }

    fun getScopePass(): String = if (prefs != null) {
        prefs?.getString(SCOPE_PASS, "")!!
    } else {
        ""
    }

    fun getTripLogHelper(): TripLogHelper? = if (prefs != null) {

        if (prefs?.getString(TRIP_LOG, "")!!.isNotEmpty()) {
            val tripLogConverter = TripLogConverter()
            tripLogConverter.stringToTripLogHelper(prefs?.getString(TRIP_LOG, "")!!)
        } else {
            null
        }
    } else {
        null
    }

    fun getForegroundServiceState(): ServiceState = if (prefs != null) {
        ServiceState.valueOf(prefs?.getString(SERVICE_STATE, ServiceState.STOPPED.name)!!)
    } else {
        ServiceState.STOPPED
    }

    fun getCustomerId(): Long = if (prefs != null) {
        prefs?.getLong(CUSTOMER_ID, -1)!!
    } else {
        0
    }

    fun getCachePreferences() = prefs?.getString("duration", "")
}