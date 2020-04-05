package com.example.mytrackingapp.tracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.mytrackingapp.R
import com.example.mytrackingapp.utility.Constants
import com.example.mytrackingapp.utility.Constants.*
import com.example.mytrackingapp.utility.Utils
import com.google.android.gms.location.LocationResult
import org.joda.time.DateTime
import org.json.JSONObject

const val TAG = "TrackingService"

class TrackingService : Service()/*IntentService(TAG)*/, LocationHelperCallback {

    lateinit var locationAgent: LocationAgent
    lateinit var helper: Helper

    private var ignoredPointsCounter = 0
    private var prevPoint: Location? = null
    private var prevTime: DateTime? = null
    private var oldActivity = STATIONARY
    private var newActivity = STATIONARY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationAgent = LocationAgent(this, this)
        helper = Helper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground()

        if (!LocationAgent.isLocationPermissionGranted(this)
            || !LocationAgent.areLocationServicesEnabled(this)) stopSelf()

        if (isRunning) return START_NOT_STICKY else isRunning = true

        locationAgent.start()
        helper.startPeriodicAlarm()
        locationAgent.registerBroadcastReceivers(true)

        return START_STICKY
    }

    override fun onLocationChanged(locationResult: LocationResult) {

        Log.d(TAG, "TrackingService.onLocationChanged")

        if (!isRunning) return

        val location = locationResult.locations[0]

        if (newActivity != "") {
            val extras: JSONObject = Utils.newJSONObject(SPEED, 0, DISTANCE, 0)
            val roguePoint: Boolean = isIrrelevantPoint(location, newActivity, extras)
            val inaccuratePoint: Boolean = location.accuracy > 75

            if (inaccuratePoint || roguePoint) {
                Log.d(TAG, "TrackingService.onLocationChanged, invalid point inaccurate = $inaccuratePoint, rogue = $roguePoint")
                ++ignoredPointsCounter
            } else {
                handleValidGeoLocation(location)
            }
        } else Log.d(TAG, "TrackingService.onLocationChanged, local new activity is empty")
    }

    //Check if new point is tooClose or too far
    private fun isIrrelevantPoint(
        location: Location,
        localNewActivity: String,
        extras: JSONObject
    ): Boolean {

        //If this is the very 1st point,
        return if (prevPoint == null) {
            prevPoint = location
            prevTime = DateTime()
            false
        } else {
            val displacement: Float = Utils.calculateDisplacement(prevPoint, location)
            val tooClosePoint =
                localNewActivity != STATIONARY && displacement.toInt() <= 50


            val speed = Utils.calculateSpeed(prevTime, DateTime(), displacement);

            //Based on the type of activity, we consider
            val tooFarPoint = speed > topSpeed(localNewActivity);

            Utils.put(extras, SPEED, speed);
            Utils.put(extras, DISTANCE, displacement)
            tooClosePoint || tooFarPoint
        }
    }

    private fun handleValidGeoLocation(
        location: Location
    ) {
        Log.d(TAG, "TrackingService.onLocationChanged, location -> lat: ${location.latitude}, lon: ${location.longitude}")
        //TODO handle location update
    }

    private fun topSpeed(newActivity: String): Int {

        if(WALKING == newActivity && !AUTOMOTIVE.equals(oldActivity) && !CYCLING.equals(oldActivity))
            return 20

        if(STATIONARY == newActivity && !AUTOMOTIVE.equals(oldActivity)  && !CYCLING.equals(oldActivity))
            return 10

        return 200

    }

    override fun onActivityChanged(activity: String) {

        Log.d(TAG, "TrackingService.onLocationChanged, activity changed -> $activity")

        val currentAndPrevActivityDifferent = newActivity != activity
        if (currentAndPrevActivityDifferent) locationAgent.start()
        newActivity = activity
        //TODO handle activity update
    }

    override fun onDestroy() {
        super.onDestroy()

        locationAgent.stopUpdates()
        locationAgent.registerBroadcastReceivers(false)
        Helper.cancelPeriodicAlarm(this)
    }

    private fun startForeground(notiId: Int = 101) {
        try {
            this.startForeground(notiId, getForegroundNotification())
        } catch (var2: Exception) {
            var2.printStackTrace()
        }
    }

    private fun getForegroundNotification(): Notification? {

        val notificationIntent: Intent
        notificationIntent = Intent()
        notificationIntent.putExtra("FROM_NOTIFICATION", true)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val largeIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.location_icon_tracking
        )
        val contentTitle = "Tracking Service"
        val contentText: String? = "You are being tracked"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.h_36x36)
            .setLargeIcon(largeIcon)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {

        val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

    companion object {

        var isRunning = false

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, TrackingService::class.java))
            }
            else context.startService(Intent(context, TrackingService::class.java))
        }

        fun interrupt(context: Context) {
            isRunning = false
            context.stopService(Intent(context, TrackingService::class.java))
        }
    }
}