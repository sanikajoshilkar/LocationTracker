package com.codecrafters.locationtracker.activities

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.codecrafters.locationtracker.R
import com.codecrafters.locationtracker.authentication.LoginActivity
import com.google.android.gms.location.*
import com.google.android.material.imageview.ShapeableImageView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private val REQUEST_NOTIFICATION_PERMISSION_CODE = 2
    private val CHANNEL_ID = "location_tracking_channel"
    private val NOTIFICATION_ID = 1
    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var routeLine: Polyline

    private var tracking = false
    private val routePoints = ArrayList<GeoPoint>()


    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                super.onBackPressed()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)


        if (intent.getBooleanExtra("restore_state", false)) {
            // Restore the state of the activity
            if (savedInstanceState != null) {
                tracking = savedInstanceState.getBoolean("tracking")
                val savedRoutePoints = savedInstanceState.getParcelableArrayList<GeoPoint>("routePoints")
                if (savedRoutePoints != null) {
                    routePoints.addAll(savedRoutePoints)
                    routeLine.setPoints(routePoints)
                }

                val centerLat = savedInstanceState.getDouble("centerLat")
                val centerLon = savedInstanceState.getDouble("centerLon")
                val zoomLevel = savedInstanceState.getDouble("zoomLevel")

                if (centerLat != 0.0 && centerLon != 0.0) {
                    map.controller.setCenter(GeoPoint(centerLat, centerLon))
                    map.controller.setZoom(zoomLevel)
                }
            }
        }
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        routeLine = Polyline()
        map.overlays.add(routeLine)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val startButton: ShapeableImageView = findViewById(R.id.btn_start)
        val stopButton: ShapeableImageView = findViewById(R.id.btn_stop)
        val shareButton: ShapeableImageView = findViewById(R.id.btn_share)
        val centerButton: ShapeableImageView = findViewById(R.id.btn_center_location)
        val logoutButton: ShapeableImageView = findViewById(R.id.btn_logout)

        startButton.setOnClickListener {
            if (checkPermissions()) {
                startLocationTracking()
                showNotification()
                startButton.isActivated = true
                stopButton.isActivated = false
                startButton.setColorFilter(Color.GREEN)
                stopButton.setColorFilter(null)
                Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopLocationTracking()
            cancelNotification()
            stopButton.isActivated = true // Glow effect
            startButton.isActivated = false
            startButton.setColorFilter(null)
            stopButton.setColorFilter(Color.GREEN)
            Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show()
        }

        shareButton.setOnClickListener {
            shareRoute()
        }

        centerButton.setOnClickListener {
            centerOnCurrentLocation()
        }

        logoutButton.setOnClickListener {

            Log.e("alerts" , "Setalerts")
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    super.onBackPressed()
                    val sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.clear()
                    editor.apply()
                    Toast.makeText(this, "Logout successfully", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()

        }

        myLocationOverlay = MyLocationNewOverlay(map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        routeLine = Polyline()
        map.overlays.add(routeLine)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (tracking) {
                    for (location in locationResult.locations) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        routePoints.add(geoPoint)
                        if (map.overlays.contains(routeLine)) { // Check if routeLine is added to the map overlays
                            routeLine.setPoints(routePoints)
                        } else {
                            routeLine = Polyline() // Initialize routeLine here
                            routeLine.setPoints(routePoints)
                            map.overlays.add(routeLine) // Add routeLine to the map overlays
                        }
                        map.controller.setCenter(geoPoint)
                    }
                }
            }
        }

        if (savedInstanceState != null) {
            tracking = savedInstanceState.getBoolean("tracking")
            val savedRoutePoints = savedInstanceState.getParcelableArrayList<GeoPoint>("routePoints")
            if (savedRoutePoints != null) {
                routePoints.addAll(savedRoutePoints)
                routeLine.setPoints(routePoints)
            }

            val centerLat = savedInstanceState.getDouble("centerLat")
            val centerLon = savedInstanceState.getDouble("centerLon")
            val zoomLevel = savedInstanceState.getDouble("zoomLevel")

            if (centerLat != 0.0 && centerLon != 0.0) {
                map.controller.setCenter(GeoPoint(centerLat, centerLon))
                map.controller.setZoom(zoomLevel)
            }
        }

        // Center on current location when activity starts
        centerOnCurrentLocation()
        createNotificationChannel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("tracking", tracking)
        outState.putParcelableArrayList("routePoints", routePoints)

        val center = map.mapCenter as GeoPoint
        outState.putDouble("centerLat", center.latitude)
        outState.putDouble("centerLon", center.longitude)
        outState.putDouble("zoomLevel", map.zoomLevelDouble)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        tracking = savedInstanceState.getBoolean("tracking")
        val savedRoutePoints = savedInstanceState.getParcelableArrayList<GeoPoint>("routePoints")
        if (savedRoutePoints != null) {
            routePoints.addAll(savedRoutePoints)
            routeLine.setPoints(routePoints)
        }

        val centerLat = savedInstanceState.getDouble("centerLat")
        val centerLon = savedInstanceState.getDouble("centerLon")
        val zoomLevel = savedInstanceState.getDouble("zoomLevel")

        if (centerLat != 0.0 && centerLon != 0.0) {
            map.controller.setCenter(GeoPoint(centerLat, centerLon))
            map.controller.setZoom(zoomLevel)
        }
    }

    private fun startLocationTracking() {
        tracking = true
        routePoints.clear()
        routeLine.setPoints(routePoints)
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationTracking() {
        tracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationTracking()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showNotification()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun shareRoute() {
        val bitmap = captureMapSnapshot()
        val file = saveBitmap(bitmap)
        file?.let {
            shareImage(file)
        }
    }

    private fun captureMapSnapshot(): Bitmap {
        val bmp = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        map.draw(canvas)
        return bmp
    }

    private fun saveBitmap(bitmap: Bitmap): File? {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "ROUTE_${sdf.format(Date())}.png"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, fileName)

        return try {
            val out = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            imageFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun shareImage(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun centerOnCurrentLocation() {
        if (checkPermissions()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        map.controller.animateTo(geoPoint)
                        map.controller.setZoom(18.0) // Set zoom level
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "Location permission is required for centering on current location", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissions()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("restore_state", true)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Location Tracking")
            .setContentText("Location tracking is started")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }


    private fun cancelNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
    }
}
