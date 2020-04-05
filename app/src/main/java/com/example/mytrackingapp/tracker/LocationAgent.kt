package com.example.mytrackingapp.tracker

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import com.example.mytrackingapp.utility.Constants.*
import com.example.mytrackingapp.utility.Utils
import com.google.android.gms.location.*

class LocationAgent(var context: Context, var locationCallback: LocationHelperCallback) {

    private var locationRequest: LocationRequest? = null
    private var locationClient: FusedLocationProviderClient? = null
    private var activityClient: ActivityRecognitionClient? = null
    private var callback: LocationCallback? = null

    private var activityRequestInterval = 10
    private var currentLocationRequestInterval = walkingLocationRequestInterval

    init {

        createLocationRequest()
        initLocationAndActivityClient()
        registerBroadcastReceivers(true)
    }

    fun start(activity: String = STATIONARY) {
        stopUpdates()
        startLocationAndActivityUpdates(activity)
    }

    fun stopUpdates() {
        try {
            this.stopLocationPolling()
            this.stopActivityRecognition()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startActivityRecognition(seconds: Int) {

        Log.d(APP_NAME, "TrackingService.startActivityRecognition")
        activityClient?.requestActivityUpdates(
            seconds * 1000L,
            getActivityRecognitionPendingIntent(context)
        )
    }

    private fun stopActivityRecognition() {

        Log.d(APP_NAME, "TrackingService.stopActivityRecognition")
        activityClient?.removeActivityUpdates(
            getActivityRecognitionPendingIntent(context)
        )
    }

    private fun createLocationRequest(localNewActivity: String = STATIONARY) {

        locationRequest = LocationRequest.create()

        if (AUTOMOTIVE.equals(localNewActivity) || CYCLING.equals(localNewActivity) || "" == localNewActivity) locationRequest?.setPriority(
            LocationRequest.PRIORITY_HIGH_ACCURACY
        ) else locationRequest?.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
        changeIntervalsAndDurationNew(localNewActivity)
        //locationRequest.setMaxWaitTime(5 * durationOneMinute);
    }

    private fun changeIntervalsAndDurationNew(localNewActivity: String) {

        currentLocationRequestInterval = getDuration(localNewActivity)
        this.locationRequest?.setInterval(currentLocationRequestInterval)
        this.locationRequest?.setFastestInterval((currentLocationRequestInterval / 2))
    }

    private fun initLocationAndActivityClient() {

        if (locationClient == null) {

            locationClient = LocationServices.getFusedLocationProviderClient(context)
            callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)

                    if (TrackingService.isRunning)//If tracking running
                        locationCallback.onLocationChanged(locationResult)
                    else
                        stopUpdates()
                }
            }
        }
        if (activityClient == null)
            activityClient = ActivityRecognition.getClient(context)
    }

    private fun startLocationAndActivityUpdates(activity: String) {

        Log.d(APP_NAME, "TrackingService.startLocationAndActivityUpdates")
        this.startLocationPolling()
        activityRequestInterval = getActivityRequestInterval(activity)
        startActivityRecognition(activityRequestInterval)
    }

    private fun getActivityRequestInterval(activity: String): Int {
        return if (activity == STATIONARY) 60 else 10 //Seconds
    }

    private fun startLocationPolling() {

        try {
            Log.d(APP_NAME, "TrackingService.startLocationPolling")
            if (!isLocationPermissionGranted(context)) return
            if (locationClient != null) {
                locationClient?.requestLocationUpdates(
                    locationRequest,
                    callback, Looper.getMainLooper()
                )
            } else Log.d(APP_NAME, "Location client is NULL")
        } catch (var2: SecurityException) {
            var2.printStackTrace()
        }
    }

    private fun stopLocationPolling() {

        Log.d(APP_NAME, "TrackingService.stopLocationPolling")
        if (locationClient != null) {
            locationClient?.removeLocationUpdates(callback)
        } else Log.d(APP_NAME, LOCATION_CLIENT_NUll)
    }

    fun registerBroadcastReceivers(flag: Boolean) {

        registerActivityReceiver(flag)
        registerGPSStatusChangeReceiver(flag)
//        registerGeofenceTriggerReceiver(flag)
    }

    private fun registerActivityReceiver(register: Boolean) {

        try {

            if (register) {
                context.registerReceiver(activityChangeReceiver, IntentFilter(HANDLE_DETECTED_ACTIVITY))
            } else context.unregisterReceiver(activityChangeReceiver)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerGPSStatusChangeReceiver(register: Boolean) {

        try {

            if (register) {
                context.registerReceiver(
                    gpsStatusChangeReceiver,
                    IntentFilter(LOCATION_PROVIDE_CHANGE_INTENT_FILTER)
                )
            } else context.unregisterReceiver(gpsStatusChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val activityChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            Log.d(APP_NAME, "TrackingService.mActivityChangeReceiver.onReceive")
            intent.getStringExtra(ACTIVITY)?.let {
                locationCallback.onActivityChanged(it)
            }
        }
    }

    private val gpsStatusChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null && intent.action == "android.location.PROVIDERS_CHANGED") {

                val notiId = 201

                if (!areLocationServicesEnabled(context)) {
                    Utils.showCustomNotification(
                        context, "Enable location for tracking",
                        notiId, Intent()
                    )
                    stopUpdates()
                } else {
                    Utils.dismissCustomNotification(context, notiId)
                    start()
                }
            }
        }
    }

    companion object {

        private const val stillLocationRequestInterval = 5 * 60 * 1000L
        private const val walkingLocationRequestInterval = 20 * 1000L
        private const val runningLocationRequestInterval = 16 * 1000L
        private const val bicycleLocationRequestInterval = 16 * 1000L
        private const val vehicleLocationRequestInterval = 8 * 1000L

        private fun getDuration(activity: String): Long {

            return when (activity) {
                AUTOMOTIVE -> vehicleLocationRequestInterval
                CYCLING -> bicycleLocationRequestInterval
                STATIONARY -> stillLocationRequestInterval
                RUNNING -> runningLocationRequestInterval
                else -> walkingLocationRequestInterval
            }
        }

        private fun getActivityRecognitionPendingIntent(
            context: Context,
            extra: String? = "TAG"
        ): PendingIntent? {
            val activityIntent = Intent(context, ActivityRecognitionService::class.java)
            activityIntent.putExtra(EXTRA, extra)
            return PendingIntent.getService(context, 103, activityIntent, FLAG_UPDATE_CURRENT)
        }

        fun isLocationPermissionGranted(context: Context): Boolean =
            checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    ||
                    checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED


        @TargetApi(Build.VERSION_CODES.KITKAT)
        fun areLocationServicesEnabled(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                var locationMode = 0
                try {
                    locationMode = Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.LOCATION_MODE
                    )
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } else {
                val locationProviders = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                )
                !TextUtils.isEmpty(locationProviders)
            }
        }
    }

}

interface LocationHelperCallback {

    fun onLocationChanged(locationResult: LocationResult)
    fun onActivityChanged(activity: String)
}