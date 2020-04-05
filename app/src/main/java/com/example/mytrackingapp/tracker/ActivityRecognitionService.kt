package com.example.mytrackingapp.tracker

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.example.mytrackingapp.utility.Constants
import com.example.mytrackingapp.utility.Utils
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import org.json.JSONObject

/**
 * Creates an IntentService.  Invoked by your subclass's constructor.
 */
class ActivityRecognitionService : IntentService(SERVICE_NAME) {

    override fun onHandleIntent(intent: Intent?) {

        Log.d(SERVICE_NAME, "ActivityRecognitionService.onHandleIntent")
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val detectedActivity = result.mostProbableActivity
            val activityName =
                getActivityName(detectedActivity)
            Log.d(
                SERVICE_NAME, "activityName: " + activityName + ", confidence: "
                        + detectedActivity.confidence
            )
            if (activityName != Constants.UNKNOWN) {

                val confidence = detectedActivity.confidence
                //Consider valid activity confidence 50+ as a valid activity
                if (confidence > 50 /*|| (confidence >= 20 && isActivityAutomotiveOrCycling(activityName))*/) {

                    Log.d(SERVICE_NAME, "ActivityRecognitionService VALID activity")
                    val activity = JSONObject()
                    Utils.put(activity, Constants.ACTIVITY, activityName)
                    Utils.put(activity, Constants.CONFIDENCE, confidence)
                    handleDetectedActivity(activity)
                } else
                    Log.d(SERVICE_NAME, "ActivityRecognitionService INVALID activity")
            }
        }
    }

    private fun handleDetectedActivity(activity: JSONObject) {
        val intent = Intent()
        intent.action = Constants.HANDLE_DETECTED_ACTIVITY
        intent.putExtra(Constants.CONFIDENCE, activity.optString(Constants.CONFIDENCE))
        intent.putExtra(Constants.ACTIVITY, activity.optString(Constants.ACTIVITY))
        sendBroadcast(intent)
    }

    companion object {

        const val SERVICE_NAME = "ActivityRecogService"

        private fun getActivityName(activity: DetectedActivity): String {
            var activityName = Constants.UNKNOWN
            when (activity.type) {
                0 -> activityName = Constants.AUTOMOTIVE
                1 -> activityName = Constants.CYCLING
                2, 7 -> activityName = Constants.WALKING
                3 -> activityName = Constants.STATIONARY
                4, 5, 6 -> {
                }
                8 -> activityName = Constants.RUNNING
                else -> {
                }
            }
            return activityName
        }
    }
}