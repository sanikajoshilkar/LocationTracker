package com.codecrafters.locationtracker.tracking

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stepCounterPrefs", Context.MODE_PRIVATE)

    fun saveStepCount(stepCount: Int) {
        val editor = prefs.edit()
        editor.putInt("stepCount", stepCount)
        editor.apply()
    }

    fun getStepCount(): Int {
        return prefs.getInt("stepCount", 0)
    }
}
