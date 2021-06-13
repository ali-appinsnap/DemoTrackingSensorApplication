package com.example.demotrackingsensorapplication.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.example.demotrackingsensorapplication.*
import com.example.demotrackingsensorapplication.R
import com.example.demotrackingsensorapplication.eventBus.ObjectDepartureHelper
import com.example.demotrackingsensorapplication.models.RecognitionData
import com.example.demotrackingsensorapplication.models.TripLogHelper
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates


class DemoService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    lateinit var prefsHelper: SharePreferencesHelper

    private lateinit var notification: Notification
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationPermission by Delegates.notNull<Int>()
    private var activityRecognitionPermission by Delegates.notNull<Int>()

    private var tripLogHelper: TripLogHelper = TripLogHelper()
    private lateinit var objectDepartureHelper: ObjectDepartureHelper

    companion object {
        const val TRIP_HELPER = "tripHelperObj"
        const val DETECTION_INTERVAL_IN_MILLISECONDS = (5 * 1000).toLong()
        const val CONFIDENCE = 70
    }

    //    For Activity Recognition
    private var mIntentService: Intent? = null
    private var mPendingIntent: PendingIntent? = null
    private var mActivityRecognitionClient: ActivityRecognitionClient? = null

    var mPlayer: MediaPlayer? = null


    override fun onBind(intent: Intent): IBinder? {
        logD(APP_TAG, "Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logD(APP_TAG, "onStartCommand executed with startId: $startId")
        prefsHelper = SharePreferencesHelper.invoke(this)

        if (intent != null) {
            val action = intent.action
            logD(APP_TAG, "using an intent with action $action")

            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                Actions.ACTIVITY_RECOGNITION.name -> {
                    handleActivityRecognitionIntent(intent)
                }
                else -> logD(APP_TAG, "This should never happen. No action in the received intent")
            }
        } else {
            logD(
                APP_TAG, "with a null intent. It has been probably restarted by the system."
            )
        }

        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        objectDepartureHelper = ObjectDepartureHelper(this)
        fusedLocationRequestSetup(setLocationListener = true)
        logD(APP_TAG, "The service has been created".toUpperCase())
        notification = createNotification(getString(R.string.tripsLocationServiceLabel))
        startForeground(1, notification)

        mActivityRecognitionClient = ActivityRecognitionClient(this)
        mIntentService = Intent(this, DemoService::class.java)
        mIntentService!!.action = Actions.ACTIVITY_RECOGNITION.name
        mPendingIntent =
            PendingIntent.getService(this, 1, mIntentService!!, PendingIntent.FLAG_UPDATE_CURRENT)
        requestActivityUpdatesButtonHandler()

    }

    override fun onDestroy() {
        super.onDestroy()
        logD(APP_TAG, "The service has been destroyed".toUpperCase())
//        removeActivityUpdatesButtonHandler()
//        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, DemoService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent =
            PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        );
    }

    @SuppressLint("MissingPermission")
    private fun startService() {
        if (isServiceStarted) {
            logD(APP_TAG, "Service Already started")
            val tLH = prefsHelper.getTripLogHelper()
            if (tLH != null) {
                objectDepartureHelper.sendParcelableBroadcast(TRIP_HELPER, tLH)
            }
            return
        }
        logD(APP_TAG, "Starting the foreground service task")
//        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        prefsHelper.updateForegroundServiceState(ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GenixService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
//                    pingFakeServer()

                    if (tripLogHelper.tripStarted)
                        getCurrentLocation()
                    /*  notification = createNotification(counter)
                      startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION)*/
                }
                delay(1 * 5 * 1000)
            }
            logD(APP_TAG, "End of the loop for the service")
        }
    }

    private fun stopService() {
        logD(APP_TAG, "Stopping the foreground service")
//        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            logD(APP_TAG, "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        prefsHelper.updateForegroundServiceState(ServiceState.STOPPED)
    }

    private fun handleActivityRecognitionIntent(intent: Intent) {
        var bestFit = 0
        val result = ActivityRecognitionResult.extractResult(intent)

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        val detectedActivities: ArrayList<DetectedActivity> =
            result.probableActivities as ArrayList<DetectedActivity>

        var activityRecognition: DetectedActivity? = null
        for (activity in detectedActivities) {
            if (bestFit < activity.confidence) {
                bestFit = activity.confidence
                logD(
                    APP_TAG,
                    "Best Fit Detected activity: " + activity.type + ", " + activity.confidence
                )
                activityRecognition = activity
            }
//            broadcastActivity(activity)
        }
        if (activityRecognition != null)
            handleUserActivity(activityRecognition.type, activityRecognition.confidence)

    }


    private fun handleUserActivity(type: Int, confidence: Int) {
        /* if (confidence < CONFIDENCE) {
             return
         }*/
        tripLogHelper.recognitionData = RecognitionData(type, confidence, "")

//        var icon: Int = R.drawable.ic_still
        when (type) {
            DetectedActivity.IN_VEHICLE -> {
                tripLogHelper.recognitionData.actionName = getString(R.string.activity_in_vehicle)
                if (!tripLogHelper.tripStarted) {
                    tripLogHelper = TripLogHelper()
                    tripLogHelper.tripStarted = true
                    notification =
                        createNotification("Trip has been started!")
                    startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
                    objectDepartureHelper.sendParcelableBroadcast(TRIP_HELPER, tripLogHelper)
                    playVoiceCommand(R.raw.sys_genix_trip_started)
                }
//                icon = R.drawable.ic_driving
            }
            DetectedActivity.TILTING -> {
                if (tripLogHelper.tripStarted) {
//                    showShort(this@DemoService, "Please Avoid mobile usage")
                    playVoiceCommand(R.raw.sys_genix_avoid_usage)

                }

//                icon = R.drawable.ic_driving
            }
            DetectedActivity.UNKNOWN -> {
                if (tripLogHelper.tripStarted) {
//                    showShort(this@DemoService, "Please Avoid mobile usage")
                }

//                icon = R.drawable.ic_driving
            }
            else -> {
                tripLogHelper.recognitionData.actionName =
                    getString(R.string.activity_in_vehicle_not)
                if (tripLogHelper.tripStarted) {
                    tripLogHelper.tripStarted = false
                    notification =
                        createNotification("Trip End, Have a nice day!")
                    startForeground(
                        1,
                        notification,
                        FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                    objectDepartureHelper.sendParcelableBroadcast(TRIP_HELPER, tripLogHelper)
                    playVoiceCommand(R.raw.sys_genix_destination_arrived)
                    prefsHelper.updateTripLog(tripLogHelper)

                }
            }
            /*   DetectedActivity.ON_BICYCLE -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_on_bicycle)
                   tripLogHelper.tripStarted = false
                   //                icon = R.drawable.ic_on_bicycle
               }
               DetectedActivity.ON_FOOT -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_on_foot)
                   tripLogHelper.tripStarted = false

   //                icon = R.drawable.ic_walking
               }
               DetectedActivity.RUNNING -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_running)
                   tripLogHelper.tripStarted = false

   //                icon = R.drawable.ic_running
               }
               DetectedActivity.STILL -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_still)
                   tripLogHelper.tripStarted = false

               }
               DetectedActivity.TILTING -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_tilting)
                   tripLogHelper.tripStarted = false

   //                icon = R.drawable.ic_tilting
               }
               DetectedActivity.WALKING -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_walking)
                   tripLogHelper.tripStarted = false

   //                icon = R.drawable.ic_walking
               }
               DetectedActivity.UNKNOWN -> {
                   tripLogHelper.recognitionData.actionName = getString(R.string.activity_unknown)
                   tripLogHelper.tripStarted = false

               }*/
        }
        Log.e(
            APP_TAG,
            "User activity: ${tripLogHelper.recognitionData.actionName}, Confidence: $confidence"
        )

    }

    private fun pingFakeServer() {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmmZ")
        val gmtTime = df.format(Date())

        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val json =
            """
                {
                    "deviceId": "$deviceId",
                    "createdAt": "$gmtTime"
                }
            """
        try {
            /*    Fuel.post("https://jsonplaceholder.typicode.com/posts")
                    .jsonBody(json)
                    .response { _, _, result ->
                        val (bytes, error) = result
                        if (bytes != null) {
                            logD("[response bytes] ${String(bytes)}")
                        } else {
                            logD("[response error] ${error?.message}")
                        }
                    }*/
        } catch (e: Exception) {
            logD(APP_TAG, "Error making the request: ${e.message}")
        }
    }

    private fun createNotification(msg: String): Notification {
        val notificationChannelId = "GENIX FOREGROUND CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "genix notifications channel",
                NotificationManager.IMPORTANCE_LOW
            ).let {
                it.description = "Genix Description"
                it.enableLights(false)
                it.enableVibration(false)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            ) else Notification.Builder(this)

        return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText(msg)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }


    //location Related Methods

    private fun fusedLocationRequestSetup(setLocationListener: Boolean) {
        locationRequest = LocationRequest()
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 3000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        if (setLocationListener)
            enableLocationListener()
    }

    private fun enableLocationListener() {
        if (!this@DemoService::fusedLocationClient.isInitialized) {
            return
        }
        locationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        activityRecognitionPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        if (locationPermission == PackageManager.PERMISSION_GRANTED && activityRecognitionPermission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location: Location = locationResult.getLastLocation()
                        if (location != null) {
                            /*latitude = location.latitude
                            longitude = location.longitude*/
                            logD(
                                APP_TAG,
                                "location update :: Lat : ${location.latitude} \nLng : ${location.longitude}"
                            )
                        }
                    }
                },
                null
            )
        }
    }

    private fun getCurrentLocation() {
        if (!this@DemoService::fusedLocationClient.isInitialized) {
            fusedLocationRequestSetup(true)
        }
        locationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        activityRecognitionPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        if (locationPermission == PackageManager.PERMISSION_GRANTED && activityRecognitionPermission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            fusedLocationClient.lastLocation
                .addOnSuccessListener {
                    if (it == null) return@addOnSuccessListener
                    if (tripLogHelper.currentLocation != null)
                        tripLogHelper.previousLocation = tripLogHelper.currentLocation
                    it.speed = mps_to_kmph(it.speed)
                    tripLogHelper.currentLocation = it
                    calculateMaxSpeed(it.speed)
                    checkOverSpeeding(it.speed)
                    if (tripLogHelper.previousLocation != null) {

                        //Calculating Distance between point
                        tripLogHelper.distanceBetween =
                            tripLogHelper.currentLocation!!.distanceTo(tripLogHelper.previousLocation)
                        //calculating speed difference between points
                        tripLogHelper.speedDifference = speedDifference(
                            tripLogHelper.previousLocation!!.speed,
                            tripLogHelper.currentLocation!!.speed
                        )
                        //checkCornering
                        checkCornering(
                            tripLogHelper.previousLocation!!.bearing,
                            tripLogHelper.currentLocation!!.bearing
                        )
                        /* tripLogHelper.directionBetween = tripLogHelper.previousLocation!!.bearingTo(
                             tripLogHelper.currentLocation!!
                         )*/
                        //calculating total distance of trip
                        tripLogHelper.tripDistance =
                            tripLogHelper.tripDistance + tripLogHelper.distanceBetween
                        //calculating Acceleration
                        checkAcceleration(
                            tripLogHelper.previousLocation!!.speed,
                            tripLogHelper.currentLocation!!.speed
                        )
                        //calculating Harsh Breaking
                        checkHarshBreaking(
                            tripLogHelper.previousLocation!!.speed,
                            tripLogHelper.currentLocation!!.speed
                        )
//                        setupTripStatus(it)
                    }
                    tripLogHelper.logMsg = logMsgBuilder(it)

//                    showLong(
//                        this@DemoService,
//                        tripLogHelper.logMsg
//                    )
                    logD(
                        APP_TAG,
                        tripLogHelper.logMsg
                    )
                    objectDepartureHelper.sendParcelableBroadcast(TRIP_HELPER, tripLogHelper)
                    // Got last known location. In some rare situations this can be null.
                }
        }
    }

    private fun logMsgBuilder(it: Location?): String {
        return when {
            it == null -> ""
            tripLogHelper.previousLocation != null -> {
                "Last Known Location::\n" +
                        "Trip Status : ${tripLogHelper.tripStarted}\nLat : ${it.latitude}\nLng : ${it.longitude}\nDistanceBtwPoints : ${tripLogHelper.distanceBetween} meters\nSpeed : ${

                            it.speed
                        } km/h\nspeedBtwPoints : ${tripLogHelper.speedDifference}\nDeviceDirection : ${it.bearing}\n" +
                        "bearingDifference : ${tripLogHelper.bearingDifference}"
            }
            tripLogHelper.previousLocation == null -> {
                "Last Known Location::\nTrip Status : ${tripLogHelper.tripStarted} \nLat : ${it.latitude}\nLng : ${it.longitude}\nDistanceBtwPoints : ${0} meters\nSpeed : ${

                    it.speed
                } km/h\nspeedBtwPoints : ${0}\nDeviceDirection : ${it.bearing}\nbearingDifference : ${0}"
            }
            else -> ""
        }
    }

    private fun calculateMaxSpeed(speed: Float) {
        if (speed > tripLogHelper.tripStats.maxSpeed) {
            tripLogHelper.tripStats.maxSpeed = speed
        }
    }

    private fun checkOverSpeeding(speed: Float) {
        if (speed > tripLogHelper.tripStats.overSpeedThresholds) {
            if (!tripLogHelper.tripStats.isOverSpeeding) {
                tripLogHelper.tripStats.isOverSpeeding = true
                tripLogHelper.tripStats.overSpeedCounter++
                playVoiceCommand(R.raw.sys_genix_over_speed)

            }
        } else {
            if (tripLogHelper.tripStats.isOverSpeeding) {
                tripLogHelper.tripStats.isOverSpeeding = false
            }
        }

    }

    private fun checkAcceleration(previousSpeed: Float, currentSpeed: Float) {
        if (currentSpeed > previousSpeed) {
            if ((currentSpeed - previousSpeed) > tripLogHelper.tripStats.accelerationThreshold) {
                if (!tripLogHelper.tripStats.isAccelerating) {
                    tripLogHelper.tripStats.isAccelerating = true
                    tripLogHelper.tripStats.accelerationCounter++
                    playVoiceCommand(R.raw.sys_genix_acceleration)

                }
            } else {
                if (tripLogHelper.tripStats.isAccelerating) {
                    tripLogHelper.tripStats.isAccelerating = false
                }
            }
        } else {
            if (tripLogHelper.tripStats.isAccelerating) {
                tripLogHelper.tripStats.isAccelerating = false
            }
        }

    }

    private fun checkHarshBreaking(previousSpeed: Float, currentSpeed: Float) {
        if (previousSpeed > currentSpeed) {
            if ((previousSpeed - currentSpeed) > tripLogHelper.tripStats.harshBreakingThreshold) {
                if (!tripLogHelper.tripStats.isHarshBreaking) {
                    tripLogHelper.tripStats.isHarshBreaking = true
                    tripLogHelper.tripStats.harshBreakingCounter++
                    playVoiceCommand(R.raw.sys_genix_harsh_breaking)

                }
            } else {
                if (tripLogHelper.tripStats.isHarshBreaking) {
                    tripLogHelper.tripStats.isHarshBreaking = false
                }
            }
        } else {
            if (tripLogHelper.tripStats.isHarshBreaking) {
                tripLogHelper.tripStats.isHarshBreaking = false
            }
        }
    }

    private fun speedDifference(previousSpeed: Float, currentSpeed: Float): Float {
        return if (previousSpeed > currentSpeed) {
            (previousSpeed - currentSpeed)
        } else {
            (currentSpeed - previousSpeed)
        }
    }


    private fun checkCornering(previousBearing: Float, currentBearing: Float) {
        tripLogHelper.bearingDifference = if (previousBearing > currentBearing) {
            (previousBearing - currentBearing)
        } else {
            (currentBearing - previousBearing)
        }

        if (tripLogHelper.bearingDifference > tripLogHelper.tripStats.corneringThreshold.toFloat()) {
            if (!tripLogHelper.tripStats.isCornering) {
                tripLogHelper.tripStats.isCornering = true
                tripLogHelper.tripStats.corneringCounter++
                playVoiceCommand(R.raw.sys_genix_cornering)

            }
        } else {
            if (tripLogHelper.tripStats.isCornering) {
                tripLogHelper.tripStats.isCornering = false
            }
        }

    }
/*

    private fun setupTripStatus(it: Location) {
        if (tripLogHelper.distanceBetween!! > 10) {
            if (!tripLogHelper.tripStarted) {
                tripLogHelper.tripStarted = true
                notification =
                    createNotification("Trip Started at 1 meter/sec speed")
                startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
            }
            tripLogHelper.counter = 0
        } else {
            if (tripLogHelper.counter >= 3) {
                if (tripLogHelper.tripStarted) {
                    tripLogHelper.tripStarted = false
                    notification =
                        createNotification("Trip End, Have a nice day!")
                    startForeground(
                        1,
                        notification,
                        FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                }
            } else {
                tripLogHelper.counter++
            }

        }
    }
*/


    private fun requestActivityUpdatesButtonHandler() {
        val task = mActivityRecognitionClient!!.requestActivityUpdates(
            DETECTION_INTERVAL_IN_MILLISECONDS,
            mPendingIntent
        )
        task.addOnSuccessListener {
            logD(APP_TAG, "Activity Recognition Tag Listener Added Successful")
        }
        task.addOnFailureListener {
            logD(APP_TAG, "Activity Recognition Tag Listener Added failed")
        }
    }

    private fun removeActivityUpdatesButtonHandler() {
        val task = mActivityRecognitionClient!!.removeActivityUpdates(
            mPendingIntent
        )
        task.addOnSuccessListener {
            logD(APP_TAG, "Activity Recognition Tag Listener Remove Successful")

        }
        task.addOnFailureListener {
            logD(APP_TAG, "Activity Recognition Tag Listener Remove failed")

        }
    }

    @Synchronized
    private fun playVoiceCommand(resource: Int) {
        if (mPlayer != null) {
            if (mPlayer!!.isPlaying) {
                return
            }
        }
        mPlayer = MediaPlayer.create(
            this@DemoService,
            resource
        )
        try {
            mPlayer!!.prepare()
        } catch (e: IllegalStateException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        mPlayer!!.start()
    }


    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean {
            logD(APP_TAG, "onDown: $event")
            return true
        }

        override fun onFling(
            event1: MotionEvent,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            logD(APP_TAG, "onFling: $event1 $event2")
            return true
        }
    }
}