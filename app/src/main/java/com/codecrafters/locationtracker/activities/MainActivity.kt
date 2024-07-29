package com.codecrafters.locationtracker.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.codecrafters.locationtracker.R
import com.codecrafters.locationtracker.fragment.ProfileFragment
import com.codecrafters.locationtracker.progress.ProgressFragment
import com.codecrafters.locationtracker.tracking.TrackingFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_tracking -> selectedFragment = TrackingFragment()
                R.id.nav_steps -> selectedFragment = ProgressFragment()
                R.id.nav_profile -> selectedFragment = ProfileFragment()
            }
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment).commit()
            }
            true
        }

        // Set default selection
        bottomNavigationView.selectedItemId = R.id.nav_tracking
    }
}
