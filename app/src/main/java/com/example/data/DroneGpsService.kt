package com.example.data

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedKmh: Double,
    val satellitesCount: Int,
    val signalStrengthPercentage: Int,
    val signalStatus: String
)

object DroneGpsService {
    // Standard coordinates for SkyBite Quantum Hangar (Bangalore)
    const val HANGAR_LAT = 12.9716
    const val HANGAR_LNG = 77.5946

    /**
     * Mocks or retrieves a pseudo-random customer landing coordinate based on the order ID.
     */
    fun getCustomerCoordinates(orderId: Int): Pair<Double, Double> {
        val random = Random(orderId.toLong())
        // Generate customer hangar roughly 1.5 to 3.5 km away from restaurant Hangar
        val latOffset = (random.nextDouble(0.015, 0.035)) * (if (random.nextBoolean()) 1 else -1)
        val lngOffset = (random.nextDouble(0.015, 0.035)) * (if (random.nextBoolean()) 1 else -1)
        return Pair(HANGAR_LAT + latOffset, HANGAR_LNG + lngOffset)
    }

    /**
     * Generates active tracking coordinates in real time based on progress of the flight.
     * Incorporates subtle micro-jitter (sensor drift) to match raw GPS tracking hardware telemetry.
     */
    fun trackDroneGps(
        progress: Float,
        status: String,
        orderId: Int
    ): GpsCoordinates {
        if (progress <= 0f || status == "Preparing order") {
            // Still in hangar calibrating gyroscope and locking satellite coordinates
            return GpsCoordinates(
                latitude = HANGAR_LAT,
                longitude = HANGAR_LNG,
                altitudeMeters = 0.0,
                speedKmh = 0.0,
                satellitesCount = Random.nextInt(7, 10),
                signalStrengthPercentage = Random.nextInt(88, 93),
                signalStatus = "CALIBRATING_SENSORS"
            )
        }

        val dest = getCustomerCoordinates(orderId)
        val destLat = dest.first
        val destLng = dest.second

        // Add subtle sensor drift/jitter (+/- 0.00008 deg) to mimic active GPS feed
        val jitterLat = (Random.nextDouble() - 0.5) * 0.00016
        val jitterLng = (Random.nextDouble() - 0.5) * 0.00016

        // Linearly interpolate current position based on flight progress
        val currentLat = HANGAR_LAT + (destLat - HANGAR_LAT) * progress + jitterLat
        val currentLng = HANGAR_LNG + (destLng - HANGAR_LNG) * progress + jitterLng

        // Dynamic speed based on flight status
        val speed = when (status) {
            "Drone dispatched" -> 45.0 + Random.nextDouble(-1.5, 2.0)
            "In transit" -> 72.0 + Random.nextDouble(-2.5, 3.5)
            "Arriving" -> 15.0 + Random.nextDouble(-1.0, 1.5)
            "Delivered" -> 0.0
            else -> 60.0
        }

        // Dynamic altitude profile (rises during ascent, drops during descent)
        val altitude = when {
            progress < 0.15f -> {
                // Ascent
                val ascentProgress = progress / 0.15f
                ascentProgress * 120.0
            }
            progress > 0.80f -> {
                // Descent
                if (status == "Delivered" || progress >= 1.0f) {
                    0.0
                } else {
                    val descentProgress = (1.0f - progress) / 0.20f
                    descentProgress * 120.0
                }
            }
            else -> {
                // Cruise altitude
                120.0 + Random.nextDouble(-1.5, 1.5)
            }
        }

        // Satellites and signal strength based on altitude and environment
        val sats = when {
            progress >= 1.0f -> 8 // Ground level has more obstruction
            progress in 0.15f..0.80f -> Random.nextInt(12, 16) // Clear sky
            else -> Random.nextInt(9, 12)
        }
        val signal = when {
            progress >= 1.0f -> Random.nextInt(85, 90)
            progress in 0.15f..0.80f -> Random.nextInt(95, 99)
            else -> Random.nextInt(90, 95)
        }

        val signalStatus = when {
            status == "Delivered" -> "MISSION_SUCCESS"
            progress >= 0.80f -> "APPROACH_TERMINAL"
            progress >= 0.15f -> "CRUISE_STRATOSPHERE"
            else -> "LAUNCH_VECTOR_ESTABLISHED"
        }

        return GpsCoordinates(
            latitude = if (progress >= 1.0f) destLat else currentLat,
            longitude = if (progress >= 1.0f) destLng else currentLng,
            altitudeMeters = altitude,
            speedKmh = speed,
            satellitesCount = sats,
            signalStrengthPercentage = signal,
            signalStatus = signalStatus
        )
    }
}
