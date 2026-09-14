package com.offgridrescue.app.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class LocationClient(private val context: Context) {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun observeLocationEnabled(): Flow<Boolean> = callbackFlow {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    launch { send(isLocationEnabled()) }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.onStart { emit(isLocationEnabled()) }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMillis: Long): Flow<Location> = callbackFlow {
        Log.d("LocationClient", "[GPS-LIFECYCLE] Requesting high accuracy location updates. Interval: ${intervalMillis}ms")
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMaxUpdateDelayMillis(intervalMillis * 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let {
                    Log.d("LocationClient", "[GPS-RX] Location received: ${it.latitude}, ${it.longitude}, acc: ${it.accuracy}")
                    launch { send(it) }
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            Log.d("LocationClient", "Stopping location updates.")
            client.removeLocationUpdates(callback)
        }
    }
}
