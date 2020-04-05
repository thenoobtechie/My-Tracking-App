package com.example.mytrackingapp.tracker

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.mytrackingapp.utility.Constants.EXTRA
import org.joda.time.DateTime

class Helper(var context: Context) {

    //To restart service if killed by OS
    fun startPeriodicAlarm() {

        cancelPeriodicAlarm(context)

        // schedule the alarm 10 mins from now, to trigger every 10 mins
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            DateTime().millis + 10 * durationOneMinute,
            10 * durationOneMinute.toLong(),
            getAlarmManagerPI(context)
        )
    }

    companion object {

        private const val durationOneMinute = 60 * 1000
        private const val durationOneHour = 60 * 60 * 1000
        private const val durationOneDay = 24 * 60 * 60 * 1000

        private const val REQ_CODE_ALARM = 101

        fun isServiceRunning(
            context: Context,
            serviceClass: Class<*>
        ): Boolean {
            val manager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        fun getAlarmManagerPI(mContext: Context, extra: String? = null): PendingIntent? {

            val intent = Intent(mContext, TrackingServiceStarterBR::class.java)
            intent.putExtra(EXTRA, extra)
            intent.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            return PendingIntent.getBroadcast(
                mContext, REQ_CODE_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun cancelPeriodicAlarm(context: Context) {

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            alarmManager?.cancel(getAlarmManagerPI(context))
        }
    }
}