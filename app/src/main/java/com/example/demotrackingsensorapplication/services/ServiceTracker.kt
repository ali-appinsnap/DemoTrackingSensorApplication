package com.example.demotrackingsensorapplication.services

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import com.example.demotrackingsensorapplication.MainActivity.Companion.REQUEST_CODE_FINE_LOCATION
import com.example.demotrackingsensorapplication.R
import com.example.demotrackingsensorapplication.SharePreferencesHelper
import com.example.demotrackingsensorapplication.logD

enum class ServiceState {
    STARTED,
    STOPPED,
}

enum class Actions {
    START,
    STOP,
    ACTIVITY_RECOGNITION
}


fun actionOnService(
    activity: Activity,
    action: Actions,
    preferences: SharePreferencesHelper
) {
    if (!checkGPS(activity)) {
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        if (checkLocationPermission(activity)) {
            if (preferences.getForegroundServiceState() == ServiceState.STOPPED && action == Actions.STOP) return
            Intent(activity, DemoService::class.java).also {
                it.action = action.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    logD("logGenix", "Starting the service in >=26 Mode")
                    activity.startForegroundService(it)
                    return@also
                }
                logD("logGenix", "Starting the service in < 26 Mode")
                activity.startService(it)
            }
        }
    } else {
        logD("logGenix", "Starting the service in < 23 Mode")
        Intent(activity, DemoService::class.java).also {
            it.action = action.name
            logD("logGenix", "Starting the service in < 23 Mode")
            activity.startService(it)
        }
        return
    }
}

fun toggleService(activity: Activity, preferences: SharePreferencesHelper) {
    var action: Actions = if (preferences.getForegroundServiceState() == ServiceState.STOPPED) {
        Actions.START
    } else {
        Actions.STOP
    }
    if (!checkGPS(activity)) {
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        if (checkLocationPermission(activity)) {
            if (preferences.getForegroundServiceState() == ServiceState.STOPPED && action == Actions.STOP) return
            Intent(activity, DemoService::class.java).also {
                it.action = action.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    logD("logGenix", "Starting the service in >=26 Mode")
                    activity.startForegroundService(it)
                    return@also
                }
                logD("logGenix", "Starting the service in < 26 Mode")
                activity.startService(it)
            }
        }
    } else {
        logD("logGenix", "Starting the service in < 23 Mode")
        Intent(activity, DemoService::class.java).also {
            it.action = action.name
            logD("logGenix", "Starting the service in < 23 Mode")
            activity.startService(it)
        }
        return
    }
}

fun checkGPS(context: Context): Boolean {
    val service =
        context.getSystemService(LOCATION_SERVICE) as LocationManager?
    val enabled = service
        ?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!

// check if enabled and if not send user to the GSP settings
// Better solution would be to display a dialog and suggesting to
// go to the settings

// check if enabled and if not send user to the GSP settings
// Better solution would be to display a dialog and suggesting to
// go to the settings
    if (!enabled!!) {
        AlertDialog.Builder(
            ContextThemeWrapper(
                context, R.style.my_dialog_theme
            )
        )
            .setTitle("Enable GPS")
            .setMessage("No Location Provider is Available, kindly Enable GPS !")
            .setPositiveButton(
                "Ok"
            ) { dialogInterface, i -> //Prompt the user once explanation has been shown
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()

        return false
    }
    return true
}


fun checkNetworkProvider(context: Context): Boolean {
    val service =
        context.getSystemService(LOCATION_SERVICE) as LocationManager?
    val enabled = service
        ?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)!!

// check if enabled and if not send user to the GSP settings
// Better solution would be to display a dialog and suggesting to
// go to the settings

// check if enabled and if not send user to the GSP settings
// Better solution would be to display a dialog and suggesting to
// go to the settings
    if (!enabled!!) {
        AlertDialog.Builder(
            ContextThemeWrapper(
                context, R.style.my_dialog_theme
            )
        )
            .setTitle("Enable Network")
            .setMessage("No Location Provider is Available, kindly Enable Network !")
            .setPositiveButton(
                "Ok"
            ) { dialogInterface, i -> //Prompt the user once explanation has been shown
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .create()
            .show()

        return false
    }
    return true
}


fun checkLocationPermission(activity: Activity): Boolean {
    //check the location permissions and return true or false.
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
        // Do something for Android 10 and above versions
        return if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //permissions granted
            true
        } else {
            //permissions NOT granted
            //if permissions are NOT granted, ask for permissions

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(
                    ContextThemeWrapper(
                        activity, R.style.my_dialog_theme
                    )
                )
                    .setTitle("Permissions request")
                    .setMessage("we need your permission for location and Activity!")
                    .setPositiveButton(
                        "Ok"
                    ) { dialogInterface, i -> //Prompt the user once explanation has been shown
                        requestPermissions(
                            activity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACTIVITY_RECOGNITION
                            ),
                            REQUEST_CODE_FINE_LOCATION
                        )
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION

                    ),
                    REQUEST_CODE_FINE_LOCATION
                )
            }
            false
        }
    } else{
        // do something for phones running an SDK before lollipop
        return if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //permissions granted
            true
        } else {
            //permissions NOT granted
            //if permissions are NOT granted, ask for permissions

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(
                    ContextThemeWrapper(
                        activity, R.style.my_dialog_theme
                    )
                )
                    .setTitle("Permissions request")
                    .setMessage("we need your permission for location and Activity!")
                    .setPositiveButton(
                        "Ok"
                    ) { dialogInterface, i -> //Prompt the user once explanation has been shown
                        requestPermissions(
                            activity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ),
                            REQUEST_CODE_FINE_LOCATION
                        )
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION

                    ),
                    REQUEST_CODE_FINE_LOCATION
                )
            }
            false
        }
    }

}
fun kmph_to_mps(kmph: Float): Float {
    return (0.277778 * kmph).toFloat()
}

// function to convert speed
// in m/sec to km/hr
fun mps_to_kmph(mps: Float): Float {
    return (3.6 * mps).toFloat()
}
