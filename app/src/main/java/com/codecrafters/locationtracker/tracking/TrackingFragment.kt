    package com.codecrafters.locationtracker.tracking

    import android.Manifest
    import android.app.AlertDialog
    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.app.PendingIntent
    import android.content.BroadcastReceiver
    import android.content.Context
    import android.content.Intent
    import android.content.IntentFilter
    import android.content.pm.PackageManager
    import android.graphics.Bitmap
    import android.graphics.Canvas
    import android.net.Uri
    import android.os.Build
    import com.codecrafters.locationtracker.R
    import android.os.Bundle
    import android.os.Environment
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ProgressBar
    import android.widget.TextView
    import android.widget.Toast
    import androidx.constraintlayout.widget.ConstraintLayout
    import androidx.core.app.NotificationCompat
    import androidx.core.content.ContextCompat
    import androidx.core.content.FileProvider
    import androidx.fragment.app.Fragment
    import androidx.fragment.app.activityViewModels
    import androidx.localbroadcastmanager.content.LocalBroadcastManager
    import com.codecrafters.locationtracker.R.*
    import com.codecrafters.locationtracker.StepsActivity
    import com.codecrafters.locationtracker.activities.MainActivity
    import com.google.android.gms.location.FusedLocationProviderClient
    import com.google.android.gms.location.LocationCallback
    import com.google.android.gms.location.LocationRequest
    import com.google.android.gms.location.LocationResult
    import com.google.android.gms.location.LocationServices
    import com.google.android.material.imageview.ShapeableImageView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.FirebaseDatabase
    import com.google.firebase.storage.FirebaseStorage
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
    import java.util.Date
    import java.util.Locale

    class TrackingFragment : Fragment() {
        private val trackingViewModel: TrackingViewModel by activityViewModels()
        private lateinit var map: MapView
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private lateinit var locationCallback: LocationCallback
        private lateinit var myLocationOverlay: MyLocationNewOverlay
        private lateinit var routeLine: Polyline
        private var tracking = false
        private val routePoints = ArrayList<GeoPoint>()
        private var totalDistance = 0.0

        private lateinit var tvSteps: TextView
        private lateinit var progressBar: ProgressBar
        private val maxSteps = 400

        private var stepCount = 0
        private var caloriesBurned = 0.0
        private val stepCountReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stepCount = intent.getIntExtra("stepCount", 0)
                updateUI()
            }
        }
        private fun sendStepCounterAction(action: String) {
            val intent = Intent(requireContext(), StepCounterService::class.java)
            intent.action = action
            requireContext().startService(intent)
        }
        private val handler = Handler(Looper.getMainLooper())
        private val runnable = object : Runnable {
            override fun run() {
                if (tracking) {
                    Log.d("Distance", "Total distance traveled: ${String.format("%.2f", totalDistance)} meters")
                    view?.let { rootView ->
                        val distanceButton: TextView? = rootView.findViewById(R.id.distance_button)
                        distanceButton?.text = "Distance: ${String.format("%.2f", totalDistance)} meters"
                    }
                }
                handler.postDelayed(this, 1000) // Log distance every 1 second
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(layout.fragment_tracking, container, false)
            tvSteps = view.findViewById(R.id.tv_steps)
            progressBar = view.findViewById(R.id.progressBar)

            val stepCount = PreferenceManager(requireContext()).getStepCount()
            if (stepCount > 0) {
                this.stepCount = stepCount
                updateUI()
            }
            return view
        }

        override fun onResume() {
            super.onResume()
            sendStepCounterAction("RESUME")
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                stepCountReceiver, IntentFilter("StepCountUpdate")
            )
        }
        override fun onPause() {
            super.onPause()
            sendStepCounterAction("PAUSE")
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(stepCountReceiver)
        }

        private fun updateUI() {
            tvSteps.text = "$stepCount"
            val progress = (stepCount.toFloat() / maxSteps.toFloat()) * 100
            progressBar.progress = progress.toInt()        }



        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            Configuration.getInstance().load(requireContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
            map = view.findViewById(R.id.mapView)
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            routeLine = Polyline()
            map.overlays.add(routeLine)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

            val startStopButton: ShapeableImageView = view.findViewById(R.id.btn_start_stop)
            val pauseButton: ShapeableImageView = view.findViewById(R.id.btn_pause)
            val shareButton: ShapeableImageView = view.findViewById(R.id.btn_share)
            val centerButton: ShapeableImageView = view.findViewById(R.id.btn_center_location)
            val stepsbutton:ConstraintLayout =view.findViewById(R.id.stepscounting)
           val distanceButton:TextView=view.findViewById(R.id.distance_button)


            var distanceDialog: AlertDialog? = null
            var isTracking = false
            var isPaused = false

            if (savedInstanceState != null) {
                tracking = savedInstanceState.getBoolean("tracking")
                val savedRoutePoints = savedInstanceState.getParcelableArrayList<GeoPoint>("routePoints")
                savedRoutePoints?.let {
                    routePoints.addAll(it)
                    routeLine.setPoints(routePoints)
                }

                val centerLat = savedInstanceState.getDouble("centerLat")
                val centerLon = savedInstanceState.getDouble("centerLon")
                val zoomLevel = savedInstanceState.getDouble("zoomLevel")
                if (centerLat != 0.0 && centerLon != 0.0) {
                    val centerPoint = GeoPoint(centerLat, centerLon)
                    map.controller.setCenter(centerPoint)
                    map.controller.setZoom(zoomLevel)
                }
            }

            centerOnCurrentLocation()
            createNotificationChannel()

//

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    if (tracking) {
                        for (location in locationResult.locations) {
                            val geoPoint = GeoPoint(location.latitude, location.longitude)
                            if (routePoints.isNotEmpty()) {
                                val previousPoint = routePoints.last()
                                val distance = calculateDistance(previousPoint, geoPoint)
                                if (distance < 10) continue // Ignore the point if it is less than 10 meters from the previous point
                            }

                            routePoints.add(geoPoint)
                            if (map.overlays.contains(routeLine)) {
                                routeLine.setPoints(routePoints)
                            } else {
                                routeLine = Polyline()
                                routeLine.setPoints(routePoints)
                                map.overlays.add(routeLine)
                            }
                            map.controller.setCenter(geoPoint)

                            if (routePoints.size > 1) {
                                val previousPoint = routePoints[routePoints.size - 2]
                                val currentPoint = routePoints.last()
                                val distance = calculateDistance(previousPoint, currentPoint)
                                totalDistance += distance
                                Log.d("Distanceeee", "Total dsddistance traveled: $totalDistance meters")
                            }
                        }
                    }
                }
            }

            trackingViewModel.routePoints.observe(viewLifecycleOwner) { points ->
                if (map.overlays.contains(routeLine)) {
                    routeLine.setPoints(points)
                } else {
                    routeLine = Polyline()
                    routeLine.setPoints(points)
                    map.overlays.add(routeLine)
                }
            }

//            trackingViewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
//                if (isTracking) {
//                    resumeLocationTracking()
//                } else {
//                    pauseLocationTracking()
//                }
//                startStopButton.setImageResource(if (isTracking) drawable.ic_stop_24 else drawable.ic_start)
//                pauseButton.setImageResource(if (isTracking) drawable.ic_pause else drawable.ic_resume)
//            }

            trackingViewModel.totalDistance.observe(viewLifecycleOwner) { distance ->
                distanceButton.contentDescription = "Total distance traveled: $distance meters"
            }

            restoreTrackingState()
            startStopButton.setOnClickListener {
                if (trackingViewModel.isTracking.value == false) {
                    trackingViewModel.startTracking()
                    startLocationTracking()
                    showNotification("Location Tracking Started")
                    Toast.makeText(requireContext(), "Tracking started", Toast.LENGTH_SHORT).show()
                    startStopButton.setImageResource(drawable.ic_stop_24)
                    pauseButton.isEnabled = true
                } else {
                    trackingViewModel.stopTracking()
                    stopLocationTracking()
                    cancelNotification()
                    Toast.makeText(requireContext(), "Tracking stopped", Toast.LENGTH_SHORT).show()
                    startStopButton.setImageResource(drawable.ic_start)
                    pauseButton.isEnabled = false
                }
            }

            stepsbutton.setOnClickListener(){
                val context = requireContext()
                val intent = Intent(context, StepsActivity::class.java)
                startActivity(intent)
            }
            pauseButton.setOnClickListener {
                if (trackingViewModel.isTracking.value == true) {
                    trackingViewModel.pauseTracking()
                    pauseLocationTracking()
                    showNotification("Location Tracking Paused")
                    Toast.makeText(requireContext(), "Tracking paused", Toast.LENGTH_SHORT).show()
                    pauseButton.setImageResource(drawable.ic_resume)
                } else {
                    trackingViewModel.resumeTracking()
                    resumeLocationTracking()
                    showNotification("Location Tracking Resumed")
                    Toast.makeText(requireContext(), "Tracking resumed", Toast.LENGTH_SHORT).show()
                    pauseButton.setImageResource(drawable.ic_pause)
                }
            }


            shareButton.setOnClickListener {
                // Capture the map snapshot
                shareRoute()
            }


            centerButton.setOnClickListener {
                centerOnCurrentLocation()
            }

//            logoutButton.setOnClickListener {
//                val builder = AlertDialog.Builder(requireContext())
//                builder.setMessage("Are you sure you want to exit?")
//                    .setCancelable(false)
//                    .setPositiveButton("Yes") { dialog, id ->
//                        val sharedPreferences = requireContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
//                        val editor = sharedPreferences.edit()
//                        editor.clear()
//                        editor.apply()
//                        Toast.makeText(requireContext(), "Logout successfully", Toast.LENGTH_SHORT).show()
//
//                        val intent = Intent(requireContext(), LoginActivity::class.java)
//                        startActivity(intent)
//                        requireActivity().finish()
//                    }
//                    .setNegativeButton("No") { dialog, id ->
//                        dialog.dismiss()
//                    }
//                val alert = builder.create()
//                alert.show()
//            }

            myLocationOverlay = MyLocationNewOverlay(map)
            myLocationOverlay.enableMyLocation()
            map.overlays.add(myLocationOverlay)
            routeLine = Polyline()
            map.overlays.add(routeLine)



            if (savedInstanceState != null) {
                tracking = savedInstanceState.getBoolean("tracking")
                val savedRoutePoints = savedInstanceState.getParcelableArrayList<GeoPoint>("routePoints")
                savedRoutePoints?.let {
                    routePoints.addAll(it)
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


        private fun startLocationTracking() {
            stepCount = 0
            totalDistance = 0.0
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
                handler.post(runnable)
                sendStepCounterAction("START")
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        private fun stopLocationTracking() {

            val startTime = trackingViewModel.startTime.value
            val stopTime = trackingViewModel.stopTime.value
            val distanceTraveled = totalDistance
            val stepsTracked = stepCount

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            val formattedStartTime = startTime?.let { dateFormat.format(Date(it)) }
            val formattedStopTime = stopTime?.let { dateFormat.format(Date(it)) }
            val message = "Are you sure you want to exit?\n" +
                    "Start Time: $formattedStartTime\n" +
                    "Stop Time: $formattedStopTime\n" +
                    "Total Distance: $distanceTraveled\n" +
                    "Steps Tracked: $stepsTracked"
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    val bitmap = captureMapSnapshot()
                    val file = saveBitmap(bitmap)
                    if (file != null) {
                        // Get the current user
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            val userId = user.uid
                            val storageRef = FirebaseStorage.getInstance().reference.child("images/$userId/${file.name}")
                            val uploadTask = storageRef.putFile(Uri.fromFile(file))
                            uploadTask.addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                    // Store the data in Firebase Realtime Database
                                    val database = FirebaseDatabase.getInstance()
                                    val ref = database.reference.child("trackingData").child(userId).push()

                                    val trackingData = hashMapOf(
                                        "imageUrl" to uri.toString(),
                                        "startTime" to formattedStartTime,
                                        "stopTime" to formattedStopTime,
                                        "stepsTracked" to stepCount,
                                        "distanceTraveled" to totalDistance
                                    )

                                    ref.setValue(trackingData).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(requireContext(), "Data shared successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(requireContext(), "Failed to share data", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }.addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
            tracking = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
            handler.removeCallbacks(runnable)
            sendStepCounterAction("PAUSE")
        }

        private fun checkPermissions(): Boolean {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun requestPermissions() {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }

        private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
            val lat1 = point1.latitude
            val lon1 = point1.longitude
            val lat2 = point2.latitude
            val lon2 = point2.longitude
            val earthRadius = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return earthRadius * c
        }

        private fun showNotification(message: String) {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(requireContext(), MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setContentTitle("Location Tracking")
                .setContentText(message)
                .setSmallIcon(drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        private fun cancelNotification() {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }

        private fun restoreTrackingState() {
            trackingViewModel.isTracking.value?.let { isTracking ->
                if (isTracking) {
                    startLocationTracking()
                }
            }
            trackingViewModel.routePoints.value?.let { points ->
                routeLine.setPoints(points)
            }
        }
        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Location Tracking"
                val descriptionText = "Location tracking notifications"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun pauseLocationTracking() {
            tracking = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
            handler.removeCallbacks(runnable)
            sendStepCounterAction("PAUSE")
        }


        private fun resumeLocationTracking() {
            tracking = true
            startLocationTracking()
            sendStepCounterAction("RESUME")
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

            // Save to the Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val imageFile = File(downloadsDir, fileName)

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
            val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }




        private fun centerOnCurrentLocation() {
            if (!checkPermissions()) {
                requestPermissions()
                return
            }

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        map.controller.setCenter(geoPoint)
                        map.controller.setZoom(18.0)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        private fun createDistanceDialog(): AlertDialog {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Distance Traveled")
            val distanceTextView = TextView(requireContext())
            builder.setView(distanceTextView)
            builder.setCancelable(false)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.setOnShowListener {
                handler.post(object : Runnable {
                    override fun run() {
                        if (tracking) {
                            distanceTextView.text = "Total distance traveled: ${String.format("%.2f", totalDistance)} meters"
                        }
                        handler.postDelayed(this, 1000)
                    }
                })
            }
            return dialog
        }

        companion object {
            private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
            private const val CHANNEL_ID = "location_tracking_channel"
            private const val NOTIFICATION_ID = 1
        }
    }
