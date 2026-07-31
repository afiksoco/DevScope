package com.devscope.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Opens the panel when the device is shaken.
 *
 * Emulator / missing-sensor edge case: if the device has no accelerometer,
 * [start] returns false and DevScope falls back to the floating bubble trigger
 * instead, so the panel always stays reachable.
 */
internal class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private companion object {
        /** ~2.6g of acceleration counts as a shake. */
        const val SHAKE_THRESHOLD_G = 2.6f

        /** Ignore repeat readings for this long, so one shake = one toggle. */
        const val DEBOUNCE_MS = 700L
    }

    private var lastShakeAt = 0L

    /** @return true if a sensor was found and listening started. */
    fun start(context: Context): Boolean {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) {
            Log.w("DevScope", "No accelerometer available; falling back to bubble trigger")
            return false
        }
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        return true
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (gForce > SHAKE_THRESHOLD_G && now - lastShakeAt > DEBOUNCE_MS) {
            lastShakeAt = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
