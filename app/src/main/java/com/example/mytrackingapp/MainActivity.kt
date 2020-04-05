package com.example.mytrackingapp

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.mytrackingapp.tracker.LocationAgent
import com.example.mytrackingapp.tracker.TrackingService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start.setOnClickListener {
            TrackingService.start(applicationContext)
        }

        stop.setOnClickListener {
            TrackingService.interrupt(applicationContext)
        }

        if (!LocationAgent.isLocationPermissionGranted(this)) {

            val array = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(array, 102)
            }
            else ActivityCompat.requestPermissions(this, array, 102)
        }

        if (!LocationAgent.areLocationServicesEnabled(this))
            Toast.makeText(this, "Enable location to start tracking", Toast.LENGTH_SHORT).show()

        /*TODO(
            "FUNCTIONALITY -> " +

                    //tracking
                    "START TRACKING" +
                    "STOP TRACKING" +

                    //DB
                    "TRIPS LIST" +
                    "DELETE TRIP" +

                    //maps
                    "MAPS - ACCESS TRACKING DATA(TRIP)"
        )*/
    }
}
