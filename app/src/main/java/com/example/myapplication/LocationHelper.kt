package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class CampusLocationResult(
    val campus: String?,
    val locationDescription: String,
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {

    private data class CampusTarget(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float = 3500f
    )

    private val campusList = listOf(
        CampusTarget("江安校区", 30.5563, 103.9965, 4000f),
        CampusTarget("望江校区", 30.6318, 104.0842, 3500f),
        CampusTarget("华西校区", 30.6436, 104.0624, 2500f)
    )

    fun getCampusDefaultCoords(campus: String): Pair<Double, Double> {
        val target = campusList.firstOrNull { campus.startsWith(it.name.take(2)) }
        return if (target != null) {
            Pair(target.latitude, target.longitude)
        } else {
            Pair(30.5563, 103.9965)
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun fetchCurrentLocation(context: Context): CampusLocationResult? {
        if (!hasLocationPermission(context)) return null

        val location = withTimeoutOrNull(6000L) {
            obtainRawLocation(context)
        } ?: return null

        return withContext(Dispatchers.IO) {
            resolveAddressAndCampus(context, location)
        }
    }

    private suspend fun obtainRawLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // 1. First check recent cache
        var bestCache: Location? = null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        for (provider in providers) {
            if (!locationManager.isProviderEnabled(provider)) continue
            try {
                @Suppress("MissingPermission")
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestCache == null || loc.time > bestCache.time) {
                    bestCache = loc
                }
            } catch (_: SecurityException) {
            }
        }

        // If cache is fresh within 2 minutes, use it directly
        val twoMinutesAgo = System.currentTimeMillis() - 2 * 60 * 1000
        if (bestCache != null && bestCache.time > twoMinutesAgo) {
            return bestCache
        }

        // 2. Request a single fresh update
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (_: Exception) {
                    }
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            var registered = false
            for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
                if (locationManager.isProviderEnabled(provider)) {
                    try {
                        @Suppress("MissingPermission")
                        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                        registered = true
                        break
                    } catch (_: SecurityException) {
                    }
                }
            }

            continuation.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {
                }
            }

            if (!registered) {
                continuation.resume(bestCache)
            }
        }
    }

    private fun resolveAddressAndCampus(context: Context, location: Location): CampusLocationResult {
        val lat = location.latitude
        val lng = location.longitude

        // Determine closest campus
        var matchedCampus: String? = null
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        for (campus in campusList) {
            Location.distanceBetween(lat, lng, campus.latitude, campus.longitude, results)
            val dist = results[0]
            if (dist < campus.radiusMeters && dist < minDistance) {
                minDistance = dist
                matchedCampus = campus.name
            }
        }

        var description = ""
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val feature = address.featureName
                    val thoroughfare = address.thoroughfare
                    val subLocality = address.subLocality

                    val parts = mutableListOf<String>()
                    if (!subLocality.isNullOrBlank() && subLocality != matchedCampus) {
                        parts.add(subLocality)
                    }
                    if (!thoroughfare.isNullOrBlank()) {
                        parts.add(thoroughfare)
                    }
                    if (!feature.isNullOrBlank() && feature != thoroughfare && feature != subLocality) {
                        parts.add(feature)
                    }

                    if (parts.isNotEmpty()) {
                        description = parts.joinToString("")
                    } else if (!address.getAddressLine(0).isNullOrBlank()) {
                        description = address.getAddressLine(0)
                            .replace(address.countryName.orEmpty(), "")
                            .replace(address.adminArea.orEmpty(), "")
                            .replace(address.locality.orEmpty(), "")
                            .trim()
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (description.isBlank()) {
            description = if (matchedCampus != null) {
                "${matchedCampus}校内"
            } else {
                "经度 ${"%.3f".format(Locale.ROOT, lng)}, 纬度 ${"%.3f".format(Locale.ROOT, lat)}"
            }
        }

        return CampusLocationResult(
            campus = matchedCampus,
            locationDescription = description,
            latitude = lat,
            longitude = lng
        )
    }
}
