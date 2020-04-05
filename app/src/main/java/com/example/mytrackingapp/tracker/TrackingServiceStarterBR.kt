package com.example.mytrackingapp.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mytrackingapp.tracker.Helper.Companion.isServiceRunning
import com.example.mytrackingapp.utility.Constants

class TrackingServiceStarterBR : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(
            Constants.APP_NAME,
            "TrackingServiceStarterBR onReceive hit"
        )
        if (shouldStartService(context) && !isServiceRunning(context, TrackingService::class.java)) TrackingService.start(context)
        else Helper.cancelPeriodicAlarm(context)
    }

    private fun shouldStartService(context: Context): Boolean {
        //TODO(NOT IMPLEMENTED)
        return false
    }
}