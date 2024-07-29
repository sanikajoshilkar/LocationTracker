package com.codecrafters.locationtracker.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount = 0
    private var isInitialStepCountSet = false
    private var stepCount = 0
    private var isPaused = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> isPaused = false
            "PAUSE" -> isPaused = true
            "RESUME" -> isPaused = false
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d("Steps", "steps")

        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER && !isPaused) {
            if (event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                Log.d("StepCounterService", "Step counter sensor accuracy is low")
                return
            }

            if (!isInitialStepCountSet) {
                initialStepCount = event.values[0].toInt()
                isInitialStepCountSet = true
            }

            val currentStepCount = event.values[0].toInt()
            stepCount = currentStepCount - initialStepCount

            // Broadcast step count
            val intent = Intent("StepCountUpdate")
            intent.putExtra("stepCount", stepCount)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            // Save step count to SharedPreferences
            PreferenceManager(this).saveStepCount(stepCount)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
